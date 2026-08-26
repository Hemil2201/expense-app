import uuid

from pydantic import BaseModel, ConfigDict


class CategoryOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    icon: str | None
    is_default: bool


class CategoryCreate(BaseModel):
    name: str
    icon: str | None = None
