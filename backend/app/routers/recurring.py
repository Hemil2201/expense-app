import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.recurring_expense import RecurringExpense
from app.models.user import User
from app.schemas.recurring import RecurringExpenseCreate, RecurringExpenseOut, RecurringExpenseUpdate

router = APIRouter(prefix="/recurring", tags=["recurring"])

VALID_FREQUENCIES = {"weekly", "fortnightly", "monthly", "yearly"}


@router.get("", response_model=list[RecurringExpenseOut])
def list_recurring(db: Session = Depends(get_db), _: User = Depends(get_current_user)) -> list[RecurringExpense]:
    return list(db.execute(select(RecurringExpense).order_by(RecurringExpense.next_run_date)).scalars())


@router.post("", response_model=RecurringExpenseOut)
def create_recurring(
    body: RecurringExpenseCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> RecurringExpense:
    if body.frequency not in VALID_FREQUENCIES:
        raise HTTPException(status_code=422, detail=f"frequency must be one of {sorted(VALID_FREQUENCIES)}")

    split_config = None
    if body.is_shared:
        split_config = {"split_type": body.split_type or "equal"}
        if body.splits:
            split_config["values"] = {str(s.user_id): str(s.value) for s in body.splits}

    template = RecurringExpense(
        id=uuid.uuid4(),
        amount=body.amount,
        currency=body.currency,
        category_id=body.category_id,
        description=body.description,
        paid_by=body.paid_by,
        is_shared=body.is_shared,
        default_split_config=split_config,
        frequency=body.frequency,
        next_run_date=body.next_run_date,
        is_active=True,
        created_by=current_user.id,
        created_at=datetime.now(timezone.utc),
    )
    db.add(template)
    db.commit()
    db.refresh(template)
    return template


@router.put("/{recurring_id}", response_model=RecurringExpenseOut)
def update_recurring(
    recurring_id: uuid.UUID,
    body: RecurringExpenseUpdate,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> RecurringExpense:
    template = db.execute(
        select(RecurringExpense).where(RecurringExpense.id == recurring_id)
    ).scalar_one_or_none()
    if template is None:
        raise HTTPException(status_code=404, detail="Recurring expense not found")

    for field, value in body.model_dump(exclude_unset=True).items():
        setattr(template, field, value)

    db.commit()
    db.refresh(template)
    return template
