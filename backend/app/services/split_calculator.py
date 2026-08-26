import uuid
from decimal import ROUND_HALF_UP, Decimal

CENT = Decimal("0.01")


class SplitValidationError(ValueError):
    pass


def _round_cents(value: Decimal) -> Decimal:
    return value.quantize(CENT, rounding=ROUND_HALF_UP)


def _reconcile_rounding(amounts: list[Decimal], total: Decimal) -> list[Decimal]:
    """Cent-level rounding can make per-user amounts miss the total by a cent
    or two. Dump the remainder onto the last entry so the split always sums
    exactly to the expense total — order is caller-determined, so this is
    deterministic for a given input order."""
    diff = total - sum(amounts)
    if diff != 0:
        amounts[-1] += diff
    return amounts


def compute_splits(
    amount: Decimal,
    split_type: str,
    user_ids: list[uuid.UUID],
    raw_values: dict[uuid.UUID, Decimal] | None = None,
) -> dict[uuid.UUID, Decimal]:
    """Returns {user_id: amount_owed}, always summing exactly to `amount`."""
    if not user_ids:
        raise SplitValidationError("A shared expense needs at least one split participant")

    if split_type == "equal":
        share = _round_cents(amount / len(user_ids))
        amounts = [share] * len(user_ids)

    elif split_type == "exact":
        _require_raw_values(user_ids, raw_values)
        amounts = [_round_cents(raw_values[uid]) for uid in user_ids]
        if sum(amounts) != _round_cents(amount):
            raise SplitValidationError(
                f"Exact split amounts ({sum(amounts)}) must sum to the expense total ({amount})"
            )
        return dict(zip(user_ids, amounts))

    elif split_type == "percentage":
        _require_raw_values(user_ids, raw_values)
        total_pct = sum(raw_values[uid] for uid in user_ids)
        if total_pct != Decimal("100"):
            raise SplitValidationError(f"Percentages must sum to 100 (got {total_pct})")
        amounts = [_round_cents(amount * raw_values[uid] / Decimal("100")) for uid in user_ids]

    elif split_type == "shares":
        _require_raw_values(user_ids, raw_values)
        total_shares = sum(raw_values[uid] for uid in user_ids)
        if total_shares <= 0:
            raise SplitValidationError("Total shares must be greater than 0")
        amounts = [_round_cents(amount * raw_values[uid] / total_shares) for uid in user_ids]

    else:
        raise SplitValidationError(f"Unknown split_type: {split_type}")

    amounts = _reconcile_rounding(amounts, _round_cents(amount))
    return dict(zip(user_ids, amounts))


def _require_raw_values(user_ids: list[uuid.UUID], raw_values: dict[uuid.UUID, Decimal] | None) -> None:
    if raw_values is None or any(uid not in raw_values for uid in user_ids):
        raise SplitValidationError("This split type requires a value per participant")
