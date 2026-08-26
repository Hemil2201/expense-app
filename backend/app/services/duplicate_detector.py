import uuid
from datetime import date, timedelta
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.expense import Expense

DATE_WINDOW_DAYS = 2


def find_duplicate(db: Session, raw_date: date, raw_amount: Decimal) -> uuid.UUID | None:
    """Exact amount match within a +/-2 day window (05_SCREENS.md). Never
    auto-merges — this just surfaces a candidate for the user to confirm or
    reject on the review screen."""
    match = db.execute(
        select(Expense.id).where(
            Expense.deleted_at.is_(None),
            Expense.amount == raw_amount,
            Expense.date >= raw_date - timedelta(days=DATE_WINDOW_DAYS),
            Expense.date <= raw_date + timedelta(days=DATE_WINDOW_DAYS),
        )
    ).scalar_one_or_none()
    return match
