from datetime import datetime

from pydantic import BaseModel


class ActivityItem(BaseModel):
    type: str  # 'expense_created' | 'comment_added'
    timestamp: datetime
    user_name: str
    message: str


class ActivityResponse(BaseModel):
    items: list[ActivityItem]
