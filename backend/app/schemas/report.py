import uuid
from decimal import Decimal

from pydantic import BaseModel


class CategoryBreakdown(BaseModel):
    category_id: uuid.UUID | None
    category_name: str
    total: Decimal


class MonthlyReport(BaseModel):
    month: int
    year: int
    total_spend: Decimal
    personal_spend: Decimal
    shared_spend: Decimal
    by_category: list[CategoryBreakdown]
