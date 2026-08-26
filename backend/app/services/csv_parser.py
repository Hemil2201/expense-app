import csv
import io
from datetime import date, datetime
from decimal import Decimal, InvalidOperation

DATE_FORMATS = ["%Y-%m-%d", "%m/%d/%Y", "%m/%d/%y", "%d/%m/%Y", "%b %d, %Y", "%B %d, %Y"]

DATE_HEADERS = {"date", "transaction date", "posted date", "trans date"}
AMOUNT_HEADERS = {"amount", "transaction amount", "amt"}
DEBIT_HEADERS = {"debit", "withdrawal", "withdrawals"}
CREDIT_HEADERS = {"credit", "deposit", "deposits", "payment"}
DESCRIPTION_HEADERS = {"description", "merchant", "payee", "name", "transaction description"}


def _parse_date(raw: str) -> date | None:
    raw = raw.strip()
    for fmt in DATE_FORMATS:
        try:
            return datetime.strptime(raw, fmt).date()
        except ValueError:
            continue
    return None


def _parse_amount(raw: str) -> Decimal | None:
    cleaned = raw.strip().replace("$", "").replace(",", "")
    negative = cleaned.startswith("(") and cleaned.endswith(")")
    if negative:
        cleaned = cleaned[1:-1]
    try:
        value = Decimal(cleaned)
    except InvalidOperation:
        return None
    return -value if negative else value


def parse_csv(raw_bytes: bytes) -> list[dict] | None:
    """Rules-based first pass for common bank/card CSV export formats.
    Returns None (triggering LLM fallback) if the header row doesn't look
    like a recognizable transaction export."""
    try:
        text = raw_bytes.decode("utf-8-sig")
    except UnicodeDecodeError:
        text = raw_bytes.decode("latin-1")

    reader = csv.DictReader(io.StringIO(text))
    if reader.fieldnames is None:
        return None

    headers = {h.strip().lower(): h for h in reader.fieldnames}

    date_col = next((headers[h] for h in DATE_HEADERS if h in headers), None)
    amount_col = next((headers[h] for h in AMOUNT_HEADERS if h in headers), None)
    debit_col = next((headers[h] for h in DEBIT_HEADERS if h in headers), None)
    credit_col = next((headers[h] for h in CREDIT_HEADERS if h in headers), None)
    description_col = next((headers[h] for h in DESCRIPTION_HEADERS if h in headers), None)

    if date_col is None or description_col is None or (amount_col is None and debit_col is None):
        return None

    transactions = []
    for row in reader:
        raw_date = _parse_date(row.get(date_col, ""))
        if raw_date is None:
            continue

        if amount_col is not None:
            # Credit-card CSV convention: charges positive, payments/credits
            # negative. Negative values are filtered out below as non-expenses.
            amount = _parse_amount(row.get(amount_col, ""))
        else:
            debit = _parse_amount(row.get(debit_col, "")) if row.get(debit_col) else None
            credit = _parse_amount(row.get(credit_col, "")) if credit_col and row.get(credit_col) else None
            # Credits (payments/refunds) aren't expenses — skip them here.
            amount = abs(debit) if debit else None
            if amount is None and credit:
                continue

        if amount is None or amount <= 0:
            continue

        transactions.append(
            {
                "raw_date": raw_date,
                "raw_description": row.get(description_col, "").strip(),
                "raw_amount": amount,
            }
        )

    return transactions if transactions else None
