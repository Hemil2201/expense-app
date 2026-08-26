import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.db import get_db
from app.models.category import Category
from app.models.user import User
from app.schemas.category import CategoryCreate, CategoryOut

router = APIRouter(prefix="/categories", tags=["categories"])


@router.get("", response_model=list[CategoryOut])
def list_categories(db: Session = Depends(get_db), _: User = Depends(get_current_user)) -> list[Category]:
    return list(db.execute(select(Category).order_by(Category.name)).scalars())


@router.post("", response_model=CategoryOut)
def create_category(
    body: CategoryCreate, db: Session = Depends(get_db), _: User = Depends(get_current_user)
) -> Category:
    category = Category(id=uuid.uuid4(), name=body.name, icon=body.icon, is_default=False)
    db.add(category)
    db.commit()
    db.refresh(category)
    return category
