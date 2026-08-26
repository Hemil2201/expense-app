from datetime import date

from app.services.recurring_service import _advance


def test_weekly_advance():
    assert _advance(date(2026, 8, 1), "weekly") == date(2026, 8, 8)


def test_fortnightly_advance():
    assert _advance(date(2026, 8, 1), "fortnightly") == date(2026, 8, 15)


def test_monthly_advance():
    assert _advance(date(2026, 8, 15), "monthly") == date(2026, 9, 15)


def test_monthly_advance_year_rollover():
    assert _advance(date(2026, 12, 15), "monthly") == date(2027, 1, 15)


def test_monthly_advance_clamps_short_month():
    # Jan 31 + 1 month -> Feb has no 31st, should clamp to Feb 28 (2026 not a leap year)
    assert _advance(date(2026, 1, 31), "monthly") == date(2026, 2, 28)


def test_yearly_advance():
    assert _advance(date(2026, 3, 10), "yearly") == date(2027, 3, 10)


def test_yearly_advance_leap_day():
    # 2028 is a leap year; 2029 is not -> clamp to Feb 28
    assert _advance(date(2028, 2, 29), "yearly") == date(2029, 2, 28)
