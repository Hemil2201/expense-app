import json
import logging
import uuid

from google import genai
from google.genai import errors as genai_errors
from google.genai import types

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MODEL = "gemini-3.6-flash"

# Merchant substring (uppercased) -> category name. First match wins.
# Not exhaustive by design — unmatched merchants fall through to the LLM,
# and genuinely ambiguous ones get flagged for the user (see 05_SCREENS.md).
MERCHANT_KEYWORDS: dict[str, str] = {
    "SAFEWAY": "Groceries",
    "TRADER JOE": "Groceries",
    "WHOLE FOODS": "Groceries",
    "KROGER": "Groceries",
    "WALMART": "Groceries",
    "COSTCO": "Groceries",
    "STARBUCKS": "Eating Out",
    "MCDONALD": "Eating Out",
    "CHIPOTLE": "Eating Out",
    "DOORDASH": "Eating Out",
    "UBER EATS": "Eating Out",
    "GRUBHUB": "Eating Out",
    "UBER": "Transport",
    "LYFT": "Transport",
    "CHEVRON": "Transport",
    "SHELL OIL": "Transport",
    "AMC": "Entertainment",
    "NETFLIX": "Subscriptions",
    "SPOTIFY": "Subscriptions",
    "HULU": "Subscriptions",
    "DISNEY+": "Subscriptions",
    "AMAZON": "Shopping",
    "TARGET": "Shopping",
    "CVS": "Health",
    "WALGREENS": "Health",
    "DELTA": "Travel",
    "UNITED AIRLINES": "Travel",
    "AIRBNB": "Travel",
    "MARRIOTT": "Travel",
    "DUNKIN": "Eating Out",
    "TACO BELL": "Eating Out",
    "KWALITY": "Eating Out",
    "O'DESI": "Eating Out",
    "PATEL BROTHERS": "Groceries",
    # Generic rather than one keyword per parking company — catches any
    # future "X PARKING" / "PARKING X" merchant without needing a new entry.
    "PARKING": "Transport",
}


def rules_based_category(description: str, categories_by_name: dict[str, uuid.UUID]) -> uuid.UUID | None:
    upper = description.upper()
    for keyword, category_name in MERCHANT_KEYWORDS.items():
        if keyword in upper and category_name in categories_by_name:
            return categories_by_name[category_name]
    return None


def _client() -> genai.Client | None:
    if not settings.gemini_api_key:
        return None
    return genai.Client(api_key=settings.gemini_api_key)


def llm_category(description: str, category_names: list[str]) -> str | None:
    """Returns a category name from `category_names`, or None if the model
    isn't confident enough (caller then flags needs_clarification). Only the
    merchant description string is sent — never the full statement."""
    client = _client()
    if client is None:
        return None

    schema = {
        "type": "object",
        "properties": {
            "category": {
                "type": ["string", "null"],
                "enum": [*category_names, None],
                "description": "Best-fitting category name, or null if none fit confidently.",
            },
        },
        "required": ["category"],
    }

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=f"Merchant/transaction description: {description!r}\n\nPick the best category.",
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_json_schema=schema,
                max_output_tokens=100,
            ),
        )
    except genai_errors.APIError as exc:
        logger.warning("LLM categorization failed: %s", exc)
        return None

    try:
        parsed = json.loads(response.text)
    except (TypeError, ValueError):
        return None
    return parsed.get("category")
