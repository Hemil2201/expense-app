import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, BackgroundTasks, Depends, File, Form, HTTPException, UploadFile
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.expense import Expense
from app.models.expense_split import ExpenseSplit
from app.models.statement import StatementTransaction, StatementUpload
from app.models.user import User
from app.schemas.statement import (
    ResolveTransactionRequest,
    StatementTransactionOut,
    StatementUploadOut,
    StatementUploadSummary,
)
from app.services.split_calculator import SplitValidationError, compute_splits
from app.services.statement_processor import process_statement
from app.services.storage_service import upload_statement_file

router = APIRouter(prefix="/statements", tags=["statements"])

ALLOWED_EXTENSIONS = (".csv", ".pdf")


@router.post("/upload", response_model=StatementUploadOut)
async def upload_statement(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    bank_name: str | None = Form(None),
    card_last4: str | None = Form(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> StatementUpload:
    if not file.filename or not file.filename.lower().endswith(ALLOWED_EXTENSIONS):
        raise HTTPException(status_code=422, detail="Only .csv and .pdf statements are supported")

    raw_bytes = await file.read()
    upload_id = uuid.uuid4()
    storage_path = f"{upload_id}/{file.filename}"
    content_type = "application/pdf" if file.filename.lower().endswith(".pdf") else "text/csv"

    try:
        file_url = upload_statement_file(storage_path, raw_bytes, content_type)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Failed to upload to storage: {exc}") from exc

    upload = StatementUpload(
        id=upload_id,
        uploaded_by=current_user.id,
        file_url=file_url,
        bank_name=bank_name,
        card_last4=card_last4,
        upload_date=datetime.now(timezone.utc),
        status="processing",
    )
    db.add(upload)
    db.commit()
    db.refresh(upload)

    background_tasks.add_task(process_statement, upload_id, raw_bytes, file.filename)
    return upload


@router.get("/{statement_id}", response_model=StatementUploadSummary)
def get_statement(
    statement_id: uuid.UUID, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> StatementUploadSummary:
    upload = db.execute(select(StatementUpload).where(StatementUpload.id == statement_id)).scalar_one_or_none()
    if upload is None:
        raise HTTPException(status_code=404, detail="Statement upload not found")

    counts = db.execute(
        select(
            func.count(StatementTransaction.id),
            func.count(StatementTransaction.resolved_expense_id),
            func.count(StatementTransaction.id).filter(
                StatementTransaction.needs_clarification.is_(True),
                StatementTransaction.resolved_expense_id.is_(None),
            ),
            func.count(StatementTransaction.id).filter(
                StatementTransaction.is_duplicate_of.isnot(None),
                StatementTransaction.resolved_expense_id.is_(None),
            ),
        ).where(StatementTransaction.statement_upload_id == statement_id)
    ).one()

    return StatementUploadSummary(
        id=upload.id,
        status=upload.status,
        total=counts[0],
        resolved=counts[1],
        needs_clarification=counts[2],
        possible_duplicates=counts[3],
        expected_total=upload.expected_transaction_count,
    )


@router.get("/{statement_id}/transactions", response_model=list[StatementTransactionOut])
def list_transactions(
    statement_id: uuid.UUID, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> list[StatementTransaction]:
    return list(
        db.execute(
            select(StatementTransaction)
            .where(StatementTransaction.statement_upload_id == statement_id)
            .order_by(StatementTransaction.raw_date)
        ).scalars()
    )


@router.post("/transactions/{transaction_id}/resolve", response_model=StatementTransactionOut)
def resolve_transaction(
    transaction_id: uuid.UUID,
    body: ResolveTransactionRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> StatementTransaction:
    txn = db.execute(
        select(StatementTransaction).where(StatementTransaction.id == transaction_id)
    ).scalar_one_or_none()
    if txn is None:
        raise HTTPException(status_code=404, detail="Statement transaction not found")
    if txn.resolved_expense_id is not None:
        raise HTTPException(status_code=409, detail="Transaction already resolved")

    if body.user_clarification_note is not None:
        txn.user_clarification_note = body.user_clarification_note
        txn.needs_clarification = False

    if txn.is_duplicate_of is not None and body.confirm_duplicate:
        # User confirmed this really is a duplicate — link to the existing
        # expense, create nothing new (05_SCREENS.md: never silently merge).
        txn.resolved_expense_id = txn.is_duplicate_of
        db.commit()
        db.refresh(txn)
        _maybe_complete_upload(db, txn.statement_upload_id)
        return txn

    if txn.raw_date is None or txn.raw_amount is None:
        raise HTTPException(status_code=422, detail="Transaction is missing a date or amount")

    upload = db.execute(select(StatementUpload).where(StatementUpload.id == txn.statement_upload_id)).scalar_one()
    paid_by = body.paid_by or upload.uploaded_by
    category_id = body.category_id or txn.matched_category_id

    now = datetime.now(timezone.utc)
    expense = Expense(
        id=uuid.uuid4(),
        amount=txn.raw_amount,
        currency="USD",
        date=txn.raw_date,
        description=txn.raw_description,
        category_id=category_id,
        paid_by=paid_by,
        is_shared=body.is_shared,
        source="statement_upload",
        source_transaction_id=txn.id,
        created_by=current_user.id,
        created_at=now,
        updated_at=now,
    )

    if body.is_shared:
        split_type = body.split_type or "equal"
        participants = list(db.execute(select(User.id).order_by(User.name)).scalars())
        raw_values = {s.user_id: s.value for s in body.splits} if body.splits else None
        try:
            amounts_owed = compute_splits(txn.raw_amount, split_type, participants, raw_values)
        except SplitValidationError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        expense.splits = [
            ExpenseSplit(id=uuid.uuid4(), user_id=user_id, split_type=split_type, amount_owed=amount)
            for user_id, amount in amounts_owed.items()
        ]

    db.add(expense)
    db.flush()
    txn.resolved_expense_id = expense.id
    db.commit()
    db.refresh(txn)

    _maybe_complete_upload(db, txn.statement_upload_id)
    return txn


def _maybe_complete_upload(db: Session, statement_upload_id: uuid.UUID) -> None:
    unresolved = db.execute(
        select(func.count(StatementTransaction.id)).where(
            StatementTransaction.statement_upload_id == statement_upload_id,
            StatementTransaction.resolved_expense_id.is_(None),
        )
    ).scalar_one()
    if unresolved == 0:
        upload = db.execute(
            select(StatementUpload).where(StatementUpload.id == statement_upload_id)
        ).scalar_one_or_none()
        if upload is not None and upload.status != "completed":
            upload.status = "completed"
            db.commit()
