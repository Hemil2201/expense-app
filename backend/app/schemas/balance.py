import uuid
from decimal import Decimal

from pydantic import BaseModel


class UserBalance(BaseModel):
    user_id: uuid.UUID
    name: str
    # Positive = this user is owed money overall; negative = this user owes.
    net_balance: Decimal


class BalanceResponse(BaseModel):
    balances: list[UserBalance]
