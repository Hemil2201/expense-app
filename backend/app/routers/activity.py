from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.expense import Expense
from app.models.expense_comment import ExpenseComment
from app.models.user import User
from app.schemas.activity import ActivityItem, ActivityResponse

router = APIRouter(prefix="/activity", tags=["activity"])


@router.get("", response_model=ActivityResponse)
def get_activity(
    limit: int = 20, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> ActivityResponse:
    """Derived from expenses + comments — no dedicated activity_log table
    (not in 03_SCHEMA.md), keeping the schema as specified."""
    users_by_id = {u.id: u.name for u in db.execute(select(User)).scalars()}

    items: list[ActivityItem] = []

    recent_expenses = db.execute(
        select(Expense).where(Expense.deleted_at.is_(None)).order_by(Expense.created_at.desc()).limit(limit)
    ).scalars()
    for e in recent_expenses:
        kind = "shared" if e.is_shared else "personal"
        items.append(
            ActivityItem(
                type="expense_created",
                timestamp=e.created_at,
                user_name=users_by_id.get(e.created_by, "Someone"),
                message=f"added a {kind} {e.currency} {e.amount} expense"
                + (f" for {e.description}" if e.description else ""),
            )
        )

    recent_comments = db.execute(
        select(ExpenseComment).order_by(ExpenseComment.created_at.desc()).limit(limit)
    ).scalars()
    for c in recent_comments:
        items.append(
            ActivityItem(
                type="comment_added",
                timestamp=c.created_at,
                user_name=users_by_id.get(c.user_id, "Someone"),
                message=f'commented: "{c.comment}"',
            )
        )

    items.sort(key=lambda i: i.timestamp, reverse=True)
    return ActivityResponse(items=items[:limit])
