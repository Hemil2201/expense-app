import logging
import uuid
from datetime import datetime, timezone

from sqlalchemy import select

from app.db import SessionLocal
from app.models.category import Category
from app.models.statement import StatementTransaction, StatementUpload
from app.services.categorizer import llm_category, rules_based_category
from app.services.csv_parser import parse_csv
from app.services.duplicate_detector import find_duplicate
from app.services.llm_parser import llm_extract_transactions, minimize_statement_text
from app.services.pdf_parser import extract_pdf_text
from app.services.storage_service import delete_statement_file

logger = logging.getLogger(__name__)


def process_statement(statement_upload_id: uuid.UUID, raw_bytes: bytes, filename: str) -> None:
    """Runs in a FastAPI BackgroundTask after the upload response is already
    sent — needs its own DB session (the request-scoped one is closed by then).
    A 2-user app doesn't need a real task queue (Celery/etc.) for this; see
    06_ROADMAP.md's judgment call on background-job weight."""
    db = SessionLocal()
    try:
        upload = db.execute(select(StatementUpload).where(StatementUpload.id == statement_upload_id)).scalar_one()

        is_pdf = filename.lower().endswith(".pdf")
        rows = None
        if not is_pdf:
            rows = parse_csv(raw_bytes)

        if rows is None:
            raw_text = extract_pdf_text(raw_bytes) if is_pdf else raw_bytes.decode("utf-8", errors="ignore")
            minimized = minimize_statement_text(raw_text)
            # minimized is None when the text doesn't confidently look like
            # transaction rows — refuse to send it to the LLM at all rather
            # than fall back to the unfiltered original (which could carry
            # name/address/account number). This statement fails to parse
            # instead; the failure message points the user at a different
            # export format.
            rows = llm_extract_transactions(minimized) if minimized is not None else []

        # The original file's bytes (name, address, full transaction
        # history) have now been read into `rows` — nothing downstream ever
        # re-reads the stored file, so there's no reason to keep it in
        # Storage past this point, success or failure.
        delete_statement_file(upload.file_url)

        if not rows:
            upload.status = "failed"
            db.commit()
            return

        # Set before the (potentially slow, one-LLM-call-per-row) loop below
        # so clients polling GET /statements/{id} can show "X of Y processed"
        # instead of a bare spinner — see expected_total in the response.
        upload.expected_transaction_count = len(rows)
        db.commit()

        categories = list(db.execute(select(Category)).scalars())
        categories_by_name = {c.name: c.id for c in categories}
        category_names = [c.name for c in categories]

        now = datetime.now(timezone.utc)
        for row in rows:
            category_id = rules_based_category(row["raw_description"], categories_by_name)
            needs_clarification = False

            if category_id is None:
                llm_pick = llm_category(row["raw_description"], category_names)
                if llm_pick and llm_pick in categories_by_name:
                    category_id = categories_by_name[llm_pick]
                else:
                    needs_clarification = True

            duplicate_id = find_duplicate(db, row["raw_date"], row["raw_amount"])

            db.add(
                StatementTransaction(
                    id=uuid.uuid4(),
                    statement_upload_id=statement_upload_id,
                    raw_date=row["raw_date"],
                    raw_description=row["raw_description"],
                    raw_amount=row["raw_amount"],
                    matched_category_id=category_id,
                    needs_clarification=needs_clarification,
                    is_duplicate_of=duplicate_id,
                    created_at=now,
                )
            )
            # Committed per-row (not batched at the end) so the transaction
            # count is visible to GET /statements/{id} while this loop is
            # still running — that's what makes "X of Y processed" possible.
            db.commit()

        upload.status = "needs_review"
        db.commit()
    except Exception:
        logger.exception("Statement processing failed for %s", statement_upload_id)
        db.rollback()
        upload = db.execute(select(StatementUpload).where(StatementUpload.id == statement_upload_id)).scalar_one_or_none()
        if upload is not None:
            upload.status = "failed"
            db.commit()
            delete_statement_file(upload.file_url)
    finally:
        db.close()
