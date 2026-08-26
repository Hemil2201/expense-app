import uuid
from datetime import date as date_type
from decimal import Decimal

from pydantic import BaseModel, ConfigDict

from app.schemas.expense import SplitInput


class RecurringExpenseCreate(BaseModel):
    amount: Decimal
    currency: str = "USD"
    category_id: uuid.UUID | None = None
    description: str | None = None
    paid_by: uuid.UUID
    is_shared: bool = False
    split_type: str | None = None
    splits: list[SplitInput] | None = None
    frequency: str
    next_run_date: date_type


class RecurringExpenseUpdate(BaseModel):
    amount: Decimal | None = None
    category_id: uuid.UUID | None = None
    description: str | None = None
    is_active: bool | None = None
    next_run_date: date_type | None = None


class RecurringExpenseOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    amount: Decimal
    currency: str
    category_id: uuid.UUID | None
    description: str | None
    paid_by: uuid.UUID
    is_shared: bool
    frequency: str
    next_run_date: date_type
    is_active: bool
