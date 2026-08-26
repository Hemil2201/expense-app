import uuid
from decimal import Decimal

from pydantic import BaseModel, ConfigDict


class BudgetOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    user_id: uuid.UUID | None
    category_id: uuid.UUID
    month: int
    year: int
    target_amount: Decimal


class BudgetSetRequest(BaseModel):
    # user_id omitted/null -> group-level target (see 03_SCHEMA.md)
    user_id: uuid.UUID | None = None
    category_id: uuid.UUID
    month: int
    year: int
    target_amount: Decimal


class BudgetUpdateRequest(BaseModel):
    target_amount: Decimal


class PersonalBudgetLine(BaseModel):
    user_id: uuid.UUID
    name: str
    target_amount: Decimal | None
    actual_spend: Decimal


class GroupBudgetLine(BaseModel):
    target_amount: Decimal | None
    actual_spend: Decimal


class CategoryBudgetSummary(BaseModel):
    category_id: uuid.UUID
    category_name: str
    personal: list[PersonalBudgetLine]
    group: GroupBudgetLine


class BudgetSummaryResponse(BaseModel):
    month: int
    year: int
    categories: list[CategoryBudgetSummary]
