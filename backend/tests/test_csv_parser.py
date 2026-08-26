from datetime import date
from decimal import Decimal

from app.services.csv_parser import parse_csv


def test_standard_date_amount_description_format():
    csv_bytes = (
        b"Date,Description,Amount\n"
        b"08/20/2026,SAFEWAY #1234,42.50\n"
        b"08/21/2026,STARBUCKS,5.75\n"
    )
    result = parse_csv(csv_bytes)
    assert result == [
        {"raw_date": date(2026, 8, 20), "raw_description": "SAFEWAY #1234", "raw_amount": Decimal("42.50")},
        {"raw_date": date(2026, 8, 21), "raw_description": "STARBUCKS", "raw_amount": Decimal("5.75")},
    ]


def test_debit_credit_columns_skips_credits():
    csv_bytes = (
        b"Transaction Date,Merchant,Debit,Credit\n"
        b"2026-08-20,AMAZON,89.99,\n"
        b"2026-08-22,PAYMENT THANK YOU,,500.00\n"
    )
    result = parse_csv(csv_bytes)
    assert len(result) == 1
    assert result[0]["raw_description"] == "AMAZON"
    assert result[0]["raw_amount"] == Decimal("89.99")


def test_dollar_signs_and_commas_stripped():
    csv_bytes = b"Date,Description,Amount\n01/15/2026,BIG PURCHASE,\"$1,234.56\"\n"
    result = parse_csv(csv_bytes)
    assert result[0]["raw_amount"] == Decimal("1234.56")


def test_unrecognized_headers_returns_none():
    csv_bytes = b"col1,col2,col3\nfoo,bar,baz\n"
    assert parse_csv(csv_bytes) is None


def test_negative_or_zero_amounts_skipped():
    csv_bytes = (
        b"Date,Description,Amount\n"
        b"08/20/2026,REFUND,-10.00\n"
        b"08/21/2026,ZERO,0.00\n"
        b"08/22/2026,REAL PURCHASE,15.00\n"
    )
    result = parse_csv(csv_bytes)
    assert len(result) == 1
    assert result[0]["raw_description"] == "REAL PURCHASE"
