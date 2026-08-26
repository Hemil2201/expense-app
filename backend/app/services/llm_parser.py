import json
import logging
import re
from datetime import datetime
from decimal import Decimal, InvalidOperation

from google import genai
from google.genai import errors as genai_errors
from google.genai import types

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MODEL = "gemini-3.6-flash"

_DATE_PREFIX = re.compile(r"^\s*(\d{1,2}/\d{1,2}(/\d{2,4})?|\d{4}-\d{2}-\d{2}|[A-Za-z]{3}\s+\d{1,2})\b")
_AMOUNT = re.compile(r"\$?-?\d[\d,]*\.\d{2}")


def minimize_statement_text(raw_text: str) -> str | None:
    """Keeps only lines shaped like a transaction row (date ... amount)
    before this text goes to Gemini's free tier — this strips the
    cardholder's name, address, account number, and account-summary
    boilerplate that raw statement text otherwise carries along, without
    having to guess every PII format a bank might use. Returns None if too
    few lines match, signaling the caller to skip the LLM call entirely
    rather than ever send unminimized statement text over the network —
    that statement then fails to parse instead of falling back to a less
    private path."""
    kept = [line for line in raw_text.splitlines() if _DATE_PREFIX.search(line) and _AMOUNT.search(line)]
    if len(kept) < 3:
        logger.warning("Statement text minimization matched only %d line(s) — refusing to send to Gemini.", len(kept))
        return None
    return "\n".join(kept)

EXTRACT_SCHEMA = {
    "type": "object",
    "properties": {
        "transactions": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "description": "Transaction date, YYYY-MM-DD"},
                    "description": {"type": "string", "description": "Merchant or transaction description"},
                    "amount": {"type": "string", "description": "Positive dollar amount charged, e.g. '42.50'"},
                },
                "required": ["date", "description", "amount"],
            },
        }
    },
    "required": ["transactions"],
}


def _client() -> genai.Client | None:
    if not settings.gemini_api_key:
        logger.warning("GEMINI_API_KEY not set — skipping LLM statement parsing fallback.")
        return None
    return genai.Client(api_key=settings.gemini_api_key)


def llm_extract_transactions(raw_text: str) -> list[dict]:
    """LLM fallback for statement text the rules-based parser couldn't
    confidently handle (messy CSVs, PDF-extracted text). Returns [] if no
    API key is configured or the call fails — caller flags rows for manual
    review rather than crashing the whole upload."""
    client = _client()
    if client is None:
        return []

    # Bank statements can be long; cap input to keep cost/latency bounded.
    truncated = raw_text[:15000]

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=(
                "Extract every purchase/charge transaction from this bank or credit card "
                "statement text. Skip payments, refunds, credits, and non-transaction lines "
                "(headers, totals, balances). Only include money the cardholder spent.\n\n"
                f"{truncated}"
            ),
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_json_schema=EXTRACT_SCHEMA,
                max_output_tokens=4096,
            ),
        )
    except genai_errors.APIError as exc:
        logger.warning("LLM statement parsing failed: %s", exc)
        return []

    try:
        parsed = json.loads(response.text)
    except (TypeError, ValueError):
        return []

    results = []
    for txn in parsed.get("transactions", []):
        try:
            parsed_date = datetime.strptime(txn["date"], "%Y-%m-%d").date()
            amount = Decimal(str(txn["amount"]).replace("$", "").replace(",", ""))
        except (ValueError, KeyError, InvalidOperation):
            continue
        if amount <= 0:
            continue
        results.append({"raw_date": parsed_date, "raw_description": txn["description"].strip(), "raw_amount": amount})

    return results
