import calendar
import uuid
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.expense import Expense
from app.models.expense_split import ExpenseSplit
from app.models.recurring_expense import RecurringExpense
from app.models.user import User
from app.services.split_calculator import compute_splits


def _add_months(d: date, months: int) -> date:
    month_index = d.month - 1 + months
    year = d.year + month_index // 12
    month = month_index % 12 + 1
    day = min(d.day, calendar.monthrange(year, month)[1])
    return date(year, month, day)


def _advance(next_run_date: date, frequency: str) -> date:
    if frequency == "weekly":
        return _add_days(next_run_date, 7)
    if frequency == "fortnightly":
        return _add_days(next_run_date, 14)
    if frequency == "monthly":
        return _add_months(next_run_date, 1)
    if frequency == "yearly":
        return _add_months(next_run_date, 12)
    raise ValueError(f"Unknown frequency: {frequency}")


def _add_days(d: date, days: int) -> date:
    return d + timedelta(days=days)


def materialize_due_recurring(db: Session) -> None:
    """Check-on-app-open materialization (see 06_ROADMAP.md open question —
    a real scheduled job is overkill for a 2-user app). Runs on every
    authenticated request via get_current_user, so it's effectively always
    fresh without needing a cron/worker process."""
    today = datetime.now(timezone.utc).date()
    due = list(
        db.execute(
            select(RecurringExpense).where(
                RecurringExpense.is_active.is_(True),
                RecurringExpense.next_run_date <= today,
            )
        ).scalars()
    )
    if not due:
        return

    participants = list(db.execute(select(User.id).order_by(User.name)).scalars())

    for template in due:
        # A recurring template can be many periods overdue (app not opened
        # in a while) — materialize each missed occurrence, not just one.
        while template.next_run_date <= today:
            now = datetime.now(timezone.utc)
            expense = Expense(
                id=uuid.uuid4(),
                amount=template.amount,
                currency=template.currency,
                date=template.next_run_date,
                description=template.description,
                category_id=template.category_id,
                paid_by=template.paid_by,
                is_shared=template.is_shared,
                source="recurring",
                created_by=template.created_by,
                created_at=now,
                updated_at=now,
            )
            if template.is_shared:
                config = template.default_split_config or {}
                split_type = config.get("split_type", "equal")
                raw_values = (
                    {uuid.UUID(k): v for k, v in config["values"].items()} if config.get("values") else None
                )
                amounts_owed = compute_splits(template.amount, split_type, participants, raw_values)
                expense.splits = [
                    ExpenseSplit(id=uuid.uuid4(), user_id=uid, split_type=split_type, amount_owed=amt)
                    for uid, amt in amounts_owed.items()
                ]
            db.add(expense)
            template.next_run_date = _advance(template.next_run_date, template.frequency)

    db.commit()
