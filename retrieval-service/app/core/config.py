from dataclasses import dataclass
from functools import lru_cache
import os
import re


KNOWN_MODEL_VECTOR_SIZES = {
    "BAAI/bge-small-en-v1.5": 384,
}


def _read_int(name: str, default: int) -> int:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default

    try:
        return int(raw_value)
    except ValueError as exc:
        raise ValueError(f"{name} must be an integer.") from exc


def _read_positive_int(name: str, default: int) -> int:
    value = _read_int(name, default)
    if value <= 0:
        raise ValueError(f"{name} must be greater than zero.")
    return value


def _read_bool(name: str, default: bool) -> bool:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default

    normalized_value = raw_value.strip().lower()
    if normalized_value in {"1", "true", "yes", "on"}:
        return True
    if normalized_value in {"0", "false", "no", "off"}:
        return False

    raise ValueError(f"{name} must be a boolean.")


def _read_allowed_model_names() -> tuple[str, ...]:
    raw_value = os.getenv("ALLOWED_MODEL_NAMES", "BAAI/bge-small-en-v1.5")
    model_names = tuple(
        model_name.strip()
        for model_name in raw_value.split(",")
        if model_name.strip()
    )

    if not model_names:
        raise ValueError("ALLOWED_MODEL_NAMES must contain at least one model.")

    unknown_models = sorted(
        model_name
        for model_name in model_names
        if model_name not in KNOWN_MODEL_VECTOR_SIZES
    )
    if unknown_models:
        raise ValueError(
            "ALLOWED_MODEL_NAMES contains unsupported models: "
            + ", ".join(unknown_models)
        )

    return model_names


def _validate_collection_name(collection_name: str) -> None:
    if not re.fullmatch(r"[A-Za-z0-9_-]+", collection_name):
        raise ValueError(
            "COLLECTION_NAME must use only letters, numbers, underscores, "
            "and hyphens."
        )


@dataclass(frozen=True)
class Settings:
    collection_name: str
    model_name: str
    allowed_model_names: tuple[str, ...]
    qdrant_host: str
    qdrant_port: int
    max_document_chars: int
    max_query_chars: int
    max_metadata_bytes: int
    max_request_bytes: int
    max_search_limit: int
    max_user_id_chars: int
    rabbitmq_host: str
    rabbitmq_port: int
    rabbitmq_username: str
    rabbitmq_password: str
    rabbitmq_virtual_host: str
    document_ingestion_queue: str
    ingestion_prefetch_count: int
    ingestion_consumer_enabled: bool
    ingestion_retry_delay_seconds: int

    @property
    def model_vector_size(self) -> int:
        return KNOWN_MODEL_VECTOR_SIZES[self.model_name]

    @classmethod
    def from_env(cls) -> "Settings":
        collection_name = os.getenv("COLLECTION_NAME", "documents")
        model_name = os.getenv("MODEL_NAME", "BAAI/bge-small-en-v1.5")
        allowed_model_names = _read_allowed_model_names()

        _validate_collection_name(collection_name)
        if model_name not in allowed_model_names:
            raise ValueError(
                f"MODEL_NAME '{model_name}' is not allowed. Configure one of: "
                + ", ".join(allowed_model_names)
            )

        return cls(
            collection_name=collection_name,
            model_name=model_name,
            allowed_model_names=allowed_model_names,
            qdrant_host=os.getenv("QDRANT_HOST", "qdrant"),
            qdrant_port=_read_positive_int("QDRANT_PORT", 6333),
            max_document_chars=_read_positive_int("MAX_DOCUMENT_CHARS", 50_000),
            max_query_chars=_read_positive_int("MAX_QUERY_CHARS", 2_000),
            max_metadata_bytes=_read_positive_int("MAX_METADATA_BYTES", 8_192),
            max_request_bytes=_read_positive_int("MAX_REQUEST_BYTES", 1_000_000),
            max_search_limit=_read_positive_int("MAX_SEARCH_LIMIT", 20),
            max_user_id_chars=_read_positive_int("MAX_USER_ID_CHARS", 128),
            rabbitmq_host=os.getenv("RABBITMQ_HOST", "rabbitmq"),
            rabbitmq_port=_read_positive_int("RABBITMQ_PORT", 5672),
            rabbitmq_username=os.getenv("RABBITMQ_USERNAME", "guest"),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", "guest"),
            rabbitmq_virtual_host=os.getenv("RABBITMQ_VIRTUAL_HOST", "/"),
            document_ingestion_queue=os.getenv(
                "DOCUMENT_INGESTION_QUEUE",
                "retrieval.document-ingestion",
            ),
            ingestion_prefetch_count=_read_positive_int(
                "INGESTION_PREFETCH_COUNT",
                1,
            ),
            ingestion_consumer_enabled=_read_bool(
                "INGESTION_CONSUMER_ENABLED",
                True,
            ),
            ingestion_retry_delay_seconds=_read_positive_int(
                "INGESTION_RETRY_DELAY_SECONDS",
                5,
            ),
        )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings.from_env()
