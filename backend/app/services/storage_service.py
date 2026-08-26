import logging

import requests

from app.core.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()
STATEMENTS_BUCKET = "statements"
AVATARS_BUCKET = "avatars"
RECEIPTS_BUCKET = "receipts"


def _upload(bucket: str, path: str, content: bytes, content_type: str) -> None:
    url = f"{settings.supabase_url}/storage/v1/object/{bucket}/{path}"
    headers = {
        "Authorization": f"Bearer {settings.supabase_service_role_key}",
        "apikey": settings.supabase_service_role_key,
        "Content-Type": content_type,
        "x-upsert": "true",
    }
    response = requests.post(url, headers=headers, data=content, timeout=30)
    response.raise_for_status()


def upload_statement_file(path: str, content: bytes, content_type: str) -> str:
    """Uploads to the private 'statements' Supabase Storage bucket. Returns
    the storage path (not a public URL — the bucket is private; the app
    never needs to re-serve the raw file, only the parsed transactions)."""
    _upload(STATEMENTS_BUCKET, path, content, content_type)
    return path


def delete_statement_file(path: str) -> None:
    """Removes the raw statement file from the private 'statements' bucket
    once its transactions have been extracted. Best-effort: a transient
    Storage failure here shouldn't fail statement processing, so this logs
    and swallows errors rather than raising."""
    url = f"{settings.supabase_url}/storage/v1/object/{STATEMENTS_BUCKET}/{path}"
    headers = {
        "Authorization": f"Bearer {settings.supabase_service_role_key}",
        "apikey": settings.supabase_service_role_key,
    }
    try:
        response = requests.delete(url, headers=headers, timeout=30)
        response.raise_for_status()
    except requests.RequestException as exc:
        logger.warning("Failed to delete statement file %s from storage: %s", path, exc)


def upload_avatar(path: str, content: bytes, content_type: str) -> str:
    """Uploads to the public 'avatars' bucket. Returns a public URL — profile
    pictures aren't sensitive, and a public bucket lets the app load them
    directly without signed URLs or auth headers on every image request."""
    _upload(AVATARS_BUCKET, path, content, content_type)
    return f"{settings.supabase_url}/storage/v1/object/public/{AVATARS_BUCKET}/{path}"


def upload_receipt(path: str, content: bytes, content_type: str) -> str:
    """Uploads to the public 'receipts' bucket. Public for the same reason as
    avatars — paths are unguessable UUIDs, and this lets the app show the
    receipt thumbnail on the expense detail screen without signed URLs."""
    _upload(RECEIPTS_BUCKET, path, content, content_type)
    return f"{settings.supabase_url}/storage/v1/object/public/{RECEIPTS_BUCKET}/{path}"
