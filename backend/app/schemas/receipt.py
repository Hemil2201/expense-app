import uuid
from datetime import date as date_type
from decimal import Decimal

from pydantic import BaseModel


class ReceiptScanResponse(BaseModel):
    date: date_type | None
    description: str | None
    amount: Decimal | None
    category_id: uuid.UUID | None
    receipt_photo_url: str
