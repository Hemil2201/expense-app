import json
import logging
from datetime import date, datetime
from decimal import Decimal, InvalidOperation

from google import genai
from google.genai import errors as genai_errors
from google.genai import types

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MODEL = "gemini-3.6-flash"  # handles vision natively, no separate vision-tier model needed

SCAN_SCHEMA = {
    "type": "object",
    "properties": {
        "date": {"type": ["string", "null"], "description": "Purchase date, YYYY-MM-DD, or null if unreadable"},
        "merchant": {"type": ["string", "null"], "description": "Merchant/store name, or null if unreadable"},
        "amount": {"type": ["string", "null"], "description": "Total amount charged, e.g. '42.50', or null if unreadable"},
    },
    "required": ["date", "merchant", "amount"],
}


class ReceiptScanResult:
    def __init__(self, date_: date | None, merchant: str | None, amount: Decimal | None):
        self.date = date_
        self.merchant = merchant
        self.amount = amount


def scan_receipt(image_bytes: bytes, media_type: str) -> ReceiptScanResult:
    """Returns whatever could be confidently read — any field can be None,
    and the caller (review screen) always lets the user fill in the rest
    rather than guessing (same principle as statement parsing)."""
    if not settings.gemini_api_key:
        return ReceiptScanResult(None, None, None)

    client = genai.Client(api_key=settings.gemini_api_key)

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=[
                types.Part.from_bytes(data=image_bytes, mime_type=media_type),
                "Extract the purchase date, merchant name, and total amount from this receipt.",
            ],
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_json_schema=SCAN_SCHEMA,
                max_output_tokens=200,
            ),
        )
    except genai_errors.APIError as exc:
        logger.warning("Receipt scan failed: %s", exc)
        return ReceiptScanResult(None, None, None)

    try:
        data = json.loads(response.text)
    except (TypeError, ValueError):
        return ReceiptScanResult(None, None, None)

    parsed_date = None
    if data.get("date"):
        try:
            parsed_date = datetime.strptime(data["date"], "%Y-%m-%d").date()
        except ValueError:
            pass

    parsed_amount = None
    if data.get("amount"):
        try:
            parsed_amount = Decimal(str(data["amount"]).replace("$", "").replace(",", ""))
        except InvalidOperation:
            pass

    return ReceiptScanResult(parsed_date, data.get("merchant"), parsed_amount)
