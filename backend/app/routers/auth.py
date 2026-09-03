from datetime import datetime, timedelta, timezone

import bcrypt
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import create_access_token, get_current_user
from app.db import get_db
from app.models.user import User
from app.schemas.auth import LoginRequest, TokenResponse
from app.schemas.user import UserOut

router = APIRouter(prefix="/auth", tags=["auth"])

MAX_FAILED_ATTEMPTS = 5
LOCKOUT_MINUTES = 15


@router.get("/users", response_model=list[UserOut])
def list_login_users(db: Session = Depends(get_db)) -> list[User]:
    """The 2 hardcoded users, for the identity-picker login screen."""
    return list(db.execute(select(User).order_by(User.name)).scalars())


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    user = db.execute(select(User).where(User.id == body.user_id)).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="Unknown user")

    now = datetime.now(timezone.utc)
    if user.locked_until is not None and user.locked_until > now:
        wait_minutes = max(1, int((user.locked_until - now).total_seconds() // 60) + 1)
        raise HTTPException(status_code=429, detail=f"Too many attempts — try again in {wait_minutes} min")

    pin_ok = user.pin_hash is not None and bcrypt.checkpw(body.pin.encode(), user.pin_hash.encode())
    if not pin_ok:
        user.failed_login_attempts += 1
        if user.failed_login_attempts >= MAX_FAILED_ATTEMPTS:
            user.locked_until = now + timedelta(minutes=LOCKOUT_MINUTES)
            user.failed_login_attempts = 0
        db.commit()
        raise HTTPException(status_code=401, detail="Incorrect PIN")

    user.failed_login_attempts = 0
    user.locked_until = None
    db.commit()

    token = create_access_token(user.id)
    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.get("/me", response_model=UserOut)
def get_me(current_user: User = Depends(get_current_user)) -> User:
    return current_user
