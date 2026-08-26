import uuid
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, Numeric, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base


class StatementUpload(Base):
    __tablename__ = "statement_uploads"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True)
    uploaded_by: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    file_url: Mapped[str] = mapped_column(String, nullable=False)
    bank_name: Mapped[str | None] = mapped_column(String, nullable=True)
    card_last4: Mapped[str | None] = mapped_column(String(4), nullable=True)
    upload_date: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String, nullable=False, default="processing")
    expected_transaction_count: Mapped[int | None] = mapped_column(Integer, nullable=True)


class StatementTransaction(Base):
    __tablename__ = "statement_transactions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True)
    statement_upload_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("statement_uploads.id"), nullable=False
    )
    raw_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    raw_description: Mapped[str | None] = mapped_column(String, nullable=True)
    raw_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2), nullable=True)
    matched_category_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("categories.id"), nullable=True
    )
    needs_clarification: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    user_clarification_note: Mapped[str | None] = mapped_column(String, nullable=True)
    is_duplicate_of: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("expenses.id"), nullable=True)
    resolved_expense_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("expenses.id"), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
