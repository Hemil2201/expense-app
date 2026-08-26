from collections import defaultdict
from decimal import Decimal

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.core.security import get_current_user
from app.db import get_db
from app.models.expense import Expense
from app.models.user import User
from app.schemas.balance import BalanceResponse, UserBalance
from app.services.currency_service import convert
from app.services.recurring_service import materialize_due_recurring

router = APIRouter(tags=["balance"])


@router.get("/balance", response_model=BalanceResponse)
def get_balance(db: Session = Depends(get_db), _: User = Depends(get_current_user)) -> BalanceResponse:
    # Dashboard hits this on every app open — piggyback the check-on-open
    # recurring-expense materialization here (see 06_ROADMAP.md judgment call).
    materialize_due_recurring(db)

    users = list(db.execute(select(User)).scalars())
    net: dict = defaultdict(lambda: Decimal("0"))

    shared_expenses = db.execute(
        select(Expense)
        .options(selectinload(Expense.splits))
        .where(Expense.is_shared.is_(True), Expense.deleted_at.is_(None))
    ).scalars()

    for expense in shared_expenses:
        # Balances are cross-user and cross-expense, so everything is
        # normalized to one base currency (see 02_FEATURES.md "real-time
        # currency conversion") before summing.
        amount = convert(expense.amount, expense.currency)
        for split in expense.splits:
            owed = convert(split.amount_owed, expense.currency)
            if split.user_id == expense.paid_by:
                # The payer fronted everyone else's share.
                net[split.user_id] += amount - owed
            else:
                net[split.user_id] -= owed

    return BalanceResponse(
        balances=[UserBalance(user_id=u.id, name=u.name, net_balance=net[u.id]) for u in users]
    )
