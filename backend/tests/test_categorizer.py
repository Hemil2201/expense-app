import uuid

from app.services.categorizer import rules_based_category

GROCERIES = uuid.uuid4()
EATING_OUT = uuid.uuid4()
CATEGORIES = {"Groceries": GROCERIES, "Eating Out": EATING_OUT}


def test_matches_known_merchant():
    assert rules_based_category("SAFEWAY #1234 SEATTLE WA", CATEGORIES) == GROCERIES


def test_case_insensitive():
    assert rules_based_category("starbucks store 555", CATEGORIES) == EATING_OUT


def test_unknown_merchant_returns_none():
    assert rules_based_category("SQ *SOME RANDOM SHOP", CATEGORIES) is None


def test_category_not_in_available_set_returns_none():
    # "AMC" keyword maps to Entertainment, which isn't in this category set
    assert rules_based_category("AMC THEATERS", CATEGORIES) is None
