import logging
import time
from decimal import Decimal

import requests

logger = logging.getLogger(__name__)

BASE_CURRENCY = "USD"
_CACHE_TTL_SECONDS = 3600  # rates don't move intraday enough to matter for this app
_rate_cache: dict[tuple[str, str], tuple[Decimal, float]] = {}


def get_rate(from_currency: str, to_currency: str) -> Decimal | None:
    """Live rate via Frankfurter (free, no API key, ECB-sourced). Returns
    None on any failure — callers fall back to treating amounts as
    unconverted rather than breaking balance/budget/report calculations
    over a third-party API hiccup."""
    if from_currency == to_currency:
        return Decimal("1")

    key = (from_currency, to_currency)
    cached = _rate_cache.get(key)
    if cached is not None and time.monotonic() - cached[1] < _CACHE_TTL_SECONDS:
        return cached[0]

    try:
        response = requests.get(
            "https://api.frankfurter.dev/v1/latest",
            params={"from": from_currency, "to": to_currency},
            timeout=5,
        )
        response.raise_for_status()
        rate = Decimal(str(response.json()["rates"][to_currency]))
    except Exception as exc:
        logger.warning("Currency conversion %s->%s failed: %s", from_currency, to_currency, exc)
        return None

    _rate_cache[key] = (rate, time.monotonic())
    return rate


def convert(amount: Decimal, from_currency: str, to_currency: str = BASE_CURRENCY) -> Decimal:
    if from_currency == to_currency:
        return amount
    rate = get_rate(from_currency, to_currency)
    if rate is None:
        return amount
    return (amount * rate).quantize(Decimal("0.01"))
