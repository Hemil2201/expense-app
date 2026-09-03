import uuid

from pydantic import BaseModel

from app.schemas.user import UserOut


class LoginRequest(BaseModel):
    user_id: uuid.UUID
    pin: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut
