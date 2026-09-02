import base64
import json
import logging
from datetime import date, datetime
from decimal import Decimal, InvalidOperation

import anthropic

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MODEL = "claude-haiku-4-5"  # handles vision natively, no separate vision-tier model needed

SCAN_SCHEMA = {
    "type": "object",
    "properties": {
        "date": {"type": ["string", "null"], "description": "Purchase date, YYYY-MM-DD, or null if unreadable"},
        "merchant": {"type": ["string", "null"], "description": "Merchant/store name, or null if unreadable"},
        "amount": {"type": ["string", "null"], "description": "Total amount charged, e.g. '42.50', or null if unreadable"},
    },
    "required": ["date", "merchant", "amount"],
    "additionalProperties": False,
}


class ReceiptScanResult:
    def __init__(self, date_: date | None, merchant: str | None, amount: Decimal | None):
        self.date = date_
        self.merchant = merchant
        self.amount = amount


def scan_receipt(image_bytes: bytes, media_type: str) -> ReceiptScanResult:
    """Returns whatever could be confidently read — any field can be None,
    and the caller (review screen) always lets the user fill in the rest
    rather than guessing (same principle as statement parsing).

    Note: Claude's vision input supports jpeg/png/gif/webp, not HEIC — the
    router's ALLOWED_CONTENT_TYPES still permits HEIC uploads, but a HEIC
    receipt will fail this call gracefully (empty result) rather than error.
    In practice Android's camera/picker overwhelmingly produce JPEG."""
    if not settings.anthropic_api_key:
        return ReceiptScanResult(None, None, None)

    client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
    image_b64 = base64.standard_b64encode(image_bytes).decode("utf-8")

    try:
        response = client.messages.create(
            model=MODEL,
            max_tokens=200,
            messages=[
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {"type": "base64", "media_type": media_type, "data": image_b64},
                        },
                        {"type": "text", "text": "Extract the purchase date, merchant name, and total amount from this receipt."},
                    ],
                }
            ],
            output_config={"format": {"type": "json_schema", "schema": SCAN_SCHEMA}},
        )
    except anthropic.APIError as exc:
        logger.warning("Receipt scan failed: %s", exc)
        return ReceiptScanResult(None, None, None)

    try:
        text = next(block.text for block in response.content if block.type == "text")
        data = json.loads(text)
    except (StopIteration, TypeError, ValueError):
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
