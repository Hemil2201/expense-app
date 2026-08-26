from fastapi import APIRouter, Depends, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.user import User
from app.schemas.user import UserOut
from app.services.storage_service import upload_avatar

router = APIRouter(prefix="/users", tags=["users"])

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}


@router.put("/me/avatar", response_model=UserOut)
async def update_avatar(
    file: UploadFile,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> User:
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=422, detail="Avatar must be a JPEG, PNG, or WebP image")

    raw_bytes = await file.read()
    extension = file.content_type.split("/")[1]
    path = f"{current_user.id}.{extension}"

    avatar_url = upload_avatar(path, raw_bytes, file.content_type)
    current_user.avatar_url = avatar_url
    db.commit()
    db.refresh(current_user)
    return current_user
