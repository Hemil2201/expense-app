import uuid
from decimal import Decimal

import pytest

from app.services.split_calculator import SplitValidationError, compute_splits

HEMIL = uuid.uuid4()
HEENAL = uuid.uuid4()
USERS = [HEMIL, HEENAL]


def test_equal_split_even():
    result = compute_splits(Decimal("50.00"), "equal", USERS)
    assert result == {HEMIL: Decimal("25.00"), HEENAL: Decimal("25.00")}


def test_equal_split_odd_cent_goes_to_last_user():
    result = compute_splits(Decimal("10.01"), "equal", USERS)
    assert sum(result.values()) == Decimal("10.01")
    assert result[HEMIL] == Decimal("5.01") or result[HEMIL] == Decimal("5.00")


def test_exact_split_must_sum_to_total():
    with pytest.raises(SplitValidationError):
        compute_splits(Decimal("50.00"), "exact", USERS, {HEMIL: Decimal("20.00"), HEENAL: Decimal("20.00")})


def test_exact_split_valid():
    result = compute_splits(Decimal("50.00"), "exact", USERS, {HEMIL: Decimal("30.00"), HEENAL: Decimal("20.00")})
    assert result == {HEMIL: Decimal("30.00"), HEENAL: Decimal("20.00")}


def test_percentage_split_must_sum_to_100():
    with pytest.raises(SplitValidationError):
        compute_splits(Decimal("100.00"), "percentage", USERS, {HEMIL: Decimal("60"), HEENAL: Decimal("30")})


def test_percentage_split_valid():
    result = compute_splits(Decimal("100.00"), "percentage", USERS, {HEMIL: Decimal("70"), HEENAL: Decimal("30")})
    assert result == {HEMIL: Decimal("70.00"), HEENAL: Decimal("30.00")}
    assert sum(result.values()) == Decimal("100.00")


def test_shares_split():
    result = compute_splits(Decimal("30.00"), "shares", USERS, {HEMIL: Decimal("2"), HEENAL: Decimal("1")})
    assert result == {HEMIL: Decimal("20.00"), HEENAL: Decimal("10.00")}


def test_shares_split_rounding_reconciles_to_total():
    result = compute_splits(Decimal("10.00"), "shares", USERS, {HEMIL: Decimal("1"), HEENAL: Decimal("2")})
    assert sum(result.values()) == Decimal("10.00")


def test_unknown_split_type_rejected():
    with pytest.raises(SplitValidationError):
        compute_splits(Decimal("10.00"), "bogus", USERS)
