import uuid
from datetime import date as date_type
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict

from app.schemas.expense import SplitInput


class StatementUploadOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    uploaded_by: uuid.UUID
    file_url: str
    bank_name: str | None
    card_last4: str | None
    upload_date: datetime
    status: str


class StatementUploadSummary(BaseModel):
    id: uuid.UUID
    status: str
    total: int
    resolved: int
    needs_clarification: int
    possible_duplicates: int
    # Only meaningful while status == "processing" — the transaction count
    # determined right after parsing, before per-row categorization starts.
    # Lets clients show "X of Y processed" instead of a bare spinner.
    expected_total: int | None = None


class StatementTransactionOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    statement_upload_id: uuid.UUID
    raw_date: date_type | None
    raw_description: str | None
    raw_amount: Decimal | None
    matched_category_id: uuid.UUID | None
    needs_clarification: bool
    user_clarification_note: str | None
    is_duplicate_of: uuid.UUID | None
    resolved_expense_id: uuid.UUID | None


class ResolveTransactionRequest(BaseModel):
    # Ignored if confirming a duplicate (no new expense is created then).
    category_id: uuid.UUID | None = None
    paid_by: uuid.UUID | None = None
    is_shared: bool = False
    split_type: str | None = None
    splits: list[SplitInput] | None = None
    user_clarification_note: str | None = None
    # Only meaningful when the transaction has is_duplicate_of set.
    confirm_duplicate: bool = False
