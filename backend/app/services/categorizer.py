import json
import logging
import uuid

import anthropic

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MODEL = "claude-haiku-4-5"

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


def _client() -> anthropic.Anthropic | None:
    if not settings.anthropic_api_key:
        return None
    return anthropic.Anthropic(api_key=settings.anthropic_api_key)


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
                "anyOf": [{"type": "string", "enum": category_names}, {"type": "null"}],
                "description": "Best-fitting category name, or null if none fit confidently.",
            },
        },
        "required": ["category"],
        "additionalProperties": False,
    }

    try:
        response = client.messages.create(
            model=MODEL,
            max_tokens=100,
            messages=[
                {
                    "role": "user",
                    "content": f"Merchant/transaction description: {description!r}\n\nPick the best category.",
                }
            ],
            output_config={"format": {"type": "json_schema", "schema": schema}},
        )
    except anthropic.APIError as exc:
        logger.warning("LLM categorization failed: %s", exc)
        return None

    try:
        text = next(block.text for block in response.content if block.type == "text")
        parsed = json.loads(text)
    except (StopIteration, TypeError, ValueError):
        return None
    return parsed.get("category")
