from app.services.llm_parser import minimize_statement_text

STATEMENT_TEXT = """JOHN DOE
123 MAIN ST, ANYTOWN CA 94000
Account Number: 4123 4567 8901 2345
Statement Period: 07/01/2026 - 07/31/2026
Minimum Payment Due: $35.00
Credit Limit: $10,000.00

07/03 STARBUCKS #4521            $6.75
07/05 PATEL BROTHERS GROCER      $142.30
07/12 UBER TRIP                  $18.40
07/20 NETFLIX.COM                $15.99
Rewards balance: 12,450 points
Thank you for being a valued cardmember."""


def test_strips_name_and_address():
    result = minimize_statement_text(STATEMENT_TEXT)
    assert "JOHN DOE" not in result
    assert "MAIN ST" not in result


def test_strips_account_number():
    result = minimize_statement_text(STATEMENT_TEXT)
    assert "4123 4567 8901 2345" not in result


def test_strips_credit_limit_and_rewards_boilerplate():
    result = minimize_statement_text(STATEMENT_TEXT)
    assert "Credit Limit" not in result
    assert "Rewards balance" not in result


def test_keeps_transaction_lines():
    result = minimize_statement_text(STATEMENT_TEXT)
    assert "STARBUCKS" in result
    assert "PATEL BROTHERS" in result
    assert "UBER TRIP" in result
    assert "NETFLIX.COM" in result


def test_unrecognizable_format_refuses_rather_than_leaking_full_text():
    prose = "This is just some unrelated prose with no date/amount rows at all, nothing transaction-shaped here."
    assert minimize_statement_text(prose) is None


def test_too_few_transaction_lines_refuses():
    # Only 2 matching lines — below the 3-line confidence threshold — should
    # refuse rather than fall back to sending the (still mostly-PII) original.
    text = f"{STATEMENT_TEXT.splitlines()[0]}\n07/03 STARBUCKS #4521 $6.75\n07/05 PATEL BROTHERS $142.30"
    assert minimize_statement_text(text) is None
