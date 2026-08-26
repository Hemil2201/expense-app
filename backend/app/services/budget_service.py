import uuid
from collections import defaultdict
from decimal import Decimal

from sqlalchemy import extract, select
from sqlalchemy.orm import Session, selectinload

from app.models.expense import Expense
from app.services.currency_service import convert


def compute_actual_spend(
    db: Session, month: int, year: int
) -> tuple[dict[tuple[uuid.UUID, uuid.UUID], Decimal], dict[uuid.UUID, Decimal]]:
    """Per 03_SCHEMA.md budget calc logic:
    - personal spend = that user's non-shared expenses + their amount_owed on shared expenses
    - group spend = full amount of shared expenses (not just one person's share)
    Returns (personal_spend[(user_id, category_id)], group_spend[category_id]).
    """
    personal_spend: dict[tuple[uuid.UUID, uuid.UUID], Decimal] = defaultdict(lambda: Decimal("0"))
    group_spend: dict[uuid.UUID, Decimal] = defaultdict(lambda: Decimal("0"))

    expenses = db.execute(
        select(Expense)
        .options(selectinload(Expense.splits))
        .where(
            Expense.deleted_at.is_(None),
            extract("month", Expense.date) == month,
            extract("year", Expense.date) == year,
        )
    ).scalars()

    for expense in expenses:
        if expense.category_id is None:
            continue
        # Budgets are set in one implicit currency, so spend is normalized
        # to the base currency before comparing (see 02_FEATURES.md
        # "real-time currency conversion").
        if expense.is_shared:
            group_spend[expense.category_id] += convert(expense.amount, expense.currency)
            for split in expense.splits:
                personal_spend[(split.user_id, expense.category_id)] += convert(split.amount_owed, expense.currency)
        else:
            personal_spend[(expense.paid_by, expense.category_id)] += convert(expense.amount, expense.currency)

    return personal_spend, group_spend
