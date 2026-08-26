import csv
import io
import uuid
from collections import defaultdict
from decimal import Decimal

from fastapi import APIRouter, Depends, Response
from sqlalchemy import extract, select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.category import Category
from app.models.expense import Expense
from app.models.user import User
from app.schemas.report import CategoryBreakdown, MonthlyReport
from app.services.currency_service import convert

router = APIRouter(prefix="/reports", tags=["reports"])


def _month_expenses(db: Session, month: int, year: int) -> list[Expense]:
    return list(
        db.execute(
            select(Expense).where(
                Expense.deleted_at.is_(None),
                extract("month", Expense.date) == month,
                extract("year", Expense.date) == year,
            )
        ).scalars()
    )


@router.get("/monthly", response_model=MonthlyReport)
def monthly_report(
    month: int, year: int, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> MonthlyReport:
    expenses = _month_expenses(db, month, year)
    categories_by_id = {c.id: c.name for c in db.execute(select(Category)).scalars()}

    personal_spend = Decimal("0")
    shared_spend = Decimal("0")
    category_totals: dict[uuid.UUID | None, Decimal] = defaultdict(lambda: Decimal("0"))

    for e in expenses:
        # Aggregates sum across expenses that may be in different
        # currencies, so normalize to the base currency first.
        amount = convert(e.amount, e.currency)
        if e.is_shared:
            shared_spend += amount
        else:
            personal_spend += amount
        category_totals[e.category_id] += amount

    by_category = [
        CategoryBreakdown(
            category_id=cat_id,
            category_name=categories_by_id.get(cat_id, "Uncategorized") if cat_id else "Uncategorized",
            total=total,
        )
        for cat_id, total in sorted(category_totals.items(), key=lambda kv: kv[1], reverse=True)
    ]

    return MonthlyReport(
        month=month,
        year=year,
        total_spend=personal_spend + shared_spend,
        personal_spend=personal_spend,
        shared_spend=shared_spend,
        by_category=by_category,
    )


@router.get("/export")
def export_csv(
    month: int, year: int, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> Response:
    expenses = _month_expenses(db, month, year)
    categories_by_id = {c.id: c.name for c in db.execute(select(Category)).scalars()}
    users_by_id = {u.id: u.name for u in db.execute(select(User)).scalars()}

    buffer = io.StringIO()
    writer = csv.writer(buffer)
    writer.writerow(["Date", "Description", "Category", "Amount", "Currency", "Personal/Shared", "Paid By"])
    for e in sorted(expenses, key=lambda e: e.date):
        writer.writerow(
            [
                e.date.isoformat(),
                e.description or "",
                categories_by_id.get(e.category_id, "Uncategorized") if e.category_id else "Uncategorized",
                str(e.amount),
                e.currency,
                "Shared" if e.is_shared else "Personal",
                users_by_id.get(e.paid_by, ""),
            ]
        )

    filename = f"expenses_{year}_{month:02d}.csv"
    return Response(
        content=buffer.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )
