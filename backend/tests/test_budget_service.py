import uuid
from decimal import Decimal

import app.models.expense_split  # noqa: F401  registers ExpenseSplit before Expense.splits is configured
from app.services.budget_service import compute_actual_spend

HEMIL = uuid.uuid4()
HEENAL = uuid.uuid4()
GROCERIES = uuid.uuid4()
EATING_OUT = uuid.uuid4()


class FakeSplit:
    def __init__(self, user_id, amount_owed):
        self.user_id = user_id
        self.amount_owed = amount_owed


class FakeExpense:
    def __init__(self, category_id, paid_by, amount, is_shared, splits=None, deleted_at=None, currency="USD"):
        self.category_id = category_id
        self.paid_by = paid_by
        self.amount = amount
        self.is_shared = is_shared
        self.splits = splits or []
        self.deleted_at = deleted_at
        self.currency = currency


def _run(monkeypatch, expenses):
    class FakeScalars:
        def __init__(self, items):
            self._items = items

        def __iter__(self):
            return iter(self._items)

    class FakeSession:
        def execute(self, *args, **kwargs):
            return type("R", (), {"scalars": lambda self=None: FakeScalars(expenses)})()

    return compute_actual_spend(FakeSession(), month=8, year=2026)


def test_personal_expense_counts_only_toward_payer(monkeypatch):
    expenses = [FakeExpense(GROCERIES, HEMIL, Decimal("40.00"), is_shared=False)]
    personal, group = _run(monkeypatch, expenses)
    assert personal[(HEMIL, GROCERIES)] == Decimal("40.00")
    assert (HEENAL, GROCERIES) not in personal
    assert group == {}


def test_shared_expense_counts_toward_both_personal_and_group():
    splits = [FakeSplit(HEMIL, Decimal("25.00")), FakeSplit(HEENAL, Decimal("25.00"))]
    expenses = [FakeExpense(EATING_OUT, HEMIL, Decimal("50.00"), is_shared=True, splits=splits)]
    personal, group = _run(None, expenses)
    assert personal[(HEMIL, EATING_OUT)] == Decimal("25.00")
    assert personal[(HEENAL, EATING_OUT)] == Decimal("25.00")
    assert group[EATING_OUT] == Decimal("50.00")


def test_group_spend_is_full_bill_not_one_share():
    splits = [FakeSplit(HEMIL, Decimal("70.00")), FakeSplit(HEENAL, Decimal("30.00"))]
    expenses = [FakeExpense(EATING_OUT, HEMIL, Decimal("100.00"), is_shared=True, splits=splits)]
    _, group = _run(None, expenses)
    assert group[EATING_OUT] == Decimal("100.00")


def test_mixed_personal_and_shared_accumulate():
    splits = [FakeSplit(HEMIL, Decimal("10.00")), FakeSplit(HEENAL, Decimal("10.00"))]
    expenses = [
        FakeExpense(GROCERIES, HEMIL, Decimal("15.00"), is_shared=False),
        FakeExpense(GROCERIES, HEMIL, Decimal("20.00"), is_shared=True, splits=splits),
    ]
    personal, group = _run(None, expenses)
    assert personal[(HEMIL, GROCERIES)] == Decimal("25.00")
    assert personal[(HEENAL, GROCERIES)] == Decimal("10.00")
    assert group[GROCERIES] == Decimal("20.00")
