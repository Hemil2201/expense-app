import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.budget import Budget
from app.models.category import Category
from app.models.user import User
from app.schemas.budget import (
    BudgetOut,
    BudgetSetRequest,
    BudgetSummaryResponse,
    BudgetUpdateRequest,
    CategoryBudgetSummary,
    GroupBudgetLine,
    PersonalBudgetLine,
)
from app.services.budget_service import compute_actual_spend

router = APIRouter(prefix="/budgets", tags=["budgets"])


@router.get("", response_model=list[BudgetOut])
def list_budgets(
    month: int,
    year: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> list[Budget]:
    return list(db.execute(select(Budget).where(Budget.month == month, Budget.year == year)).scalars())


@router.post("", response_model=BudgetOut)
def set_budget(
    body: BudgetSetRequest, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> Budget:
    """Upsert: setting a target for a category/month/level that already has
    one updates it in place, rather than erroring on the DB unique constraint."""
    user_filter = Budget.user_id.is_(None) if body.user_id is None else Budget.user_id == body.user_id
    existing = db.execute(
        select(Budget).where(
            user_filter,
            Budget.category_id == body.category_id,
            Budget.month == body.month,
            Budget.year == body.year,
        )
    ).scalar_one_or_none()

    if existing is not None:
        existing.target_amount = body.target_amount
        db.commit()
        db.refresh(existing)
        return existing

    budget = Budget(
        id=uuid.uuid4(),
        user_id=body.user_id,
        category_id=body.category_id,
        month=body.month,
        year=body.year,
        target_amount=body.target_amount,
    )
    db.add(budget)
    db.commit()
    db.refresh(budget)
    return budget


@router.put("/{budget_id}", response_model=BudgetOut)
def update_budget(
    budget_id: uuid.UUID,
    body: BudgetUpdateRequest,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> Budget:
    budget = db.execute(select(Budget).where(Budget.id == budget_id)).scalar_one_or_none()
    if budget is None:
        raise HTTPException(status_code=404, detail="Budget not found")
    budget.target_amount = body.target_amount
    db.commit()
    db.refresh(budget)
    return budget


@router.get("/summary", response_model=BudgetSummaryResponse)
def budget_summary(
    month: int,
    year: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> BudgetSummaryResponse:
    categories = list(db.execute(select(Category).order_by(Category.name)).scalars())
    users = list(db.execute(select(User).order_by(User.name)).scalars())
    budgets = list(db.execute(select(Budget).where(Budget.month == month, Budget.year == year)).scalars())
    personal_spend, group_spend = compute_actual_spend(db, month, year)

    personal_targets: dict[tuple[uuid.UUID, uuid.UUID], object] = {}
    group_targets: dict[uuid.UUID, object] = {}
    for b in budgets:
        if b.user_id is None:
            group_targets[b.category_id] = b.target_amount
        else:
            personal_targets[(b.user_id, b.category_id)] = b.target_amount

    category_summaries = []
    for category in categories:
        personal_lines = [
            PersonalBudgetLine(
                user_id=user.id,
                name=user.name,
                target_amount=personal_targets.get((user.id, category.id)),
                actual_spend=personal_spend.get((user.id, category.id), 0),
            )
            for user in users
        ]
        group_line = GroupBudgetLine(
            target_amount=group_targets.get(category.id),
            actual_spend=group_spend.get(category.id, 0),
        )
        category_summaries.append(
            CategoryBudgetSummary(
                category_id=category.id,
                category_name=category.name,
                personal=personal_lines,
                group=group_line,
            )
        )

    return BudgetSummaryResponse(month=month, year=year, categories=category_summaries)
