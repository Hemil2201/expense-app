import uuid

from fastapi import APIRouter, Depends, HTTPException, UploadFile
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.category import Category
from app.models.user import User
from app.schemas.receipt import ReceiptScanResponse
from app.services.categorizer import rules_based_category
from app.services.receipt_scanner import scan_receipt
from app.services.storage_service import upload_receipt

router = APIRouter(prefix="/receipts", tags=["receipts"])

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/heic"}


@router.post("/scan", response_model=ReceiptScanResponse)
async def scan(
    file: UploadFile,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> ReceiptScanResponse:
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=422, detail="Receipt must be a JPEG, PNG, WebP, or HEIC image")

    raw_bytes = await file.read()
    extension = file.content_type.split("/")[1]
    path = f"{current_user.id}/{uuid.uuid4()}.{extension}"
    photo_url = upload_receipt(path, raw_bytes, file.content_type)

    result = scan_receipt(raw_bytes, file.content_type)

    category_id = None
    if result.merchant:
        categories_by_name = {c.name: c.id for c in db.execute(select(Category)).scalars()}
        category_id = rules_based_category(result.merchant, categories_by_name)

    return ReceiptScanResponse(
        date=result.date,
        description=result.merchant,
        amount=result.amount,
        category_id=category_id,
        receipt_photo_url=photo_url,
    )
