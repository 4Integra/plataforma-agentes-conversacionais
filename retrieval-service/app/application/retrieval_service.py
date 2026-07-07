import json
from uuid import uuid4

from app.core.config import Settings
from app.domain.entities import DocumentToIndex, EnqueuedDocument, IndexedDocument
from app.domain.entities import SearchDocuments
from app.domain.entities import SearchResult
from app.domain.exceptions import DocumentTooLargeError, InvalidLimitError
from app.domain.exceptions import InvalidDocumentError, InvalidUserError
from app.domain.exceptions import MetadataTooLargeError
from app.domain.exceptions import QueryTooLargeError
from app.domain.ports import IngestionQueuePublisher, RetrievalRepository


class RetrievalService:
    def __init__(
        self,
        settings: Settings,
        repository: RetrievalRepository,
        ingestion_queue: IngestionQueuePublisher,
    ) -> None:
        self._settings = settings
        self._repository = repository
        self._ingestion_queue = ingestion_queue

    def ensure_ready(self) -> None:
        self._repository.ensure_collection()

    def enqueue_document(
        self,
        user_id: str,
        text: str,
        metadata: dict,
    ) -> EnqueuedDocument:
        document = DocumentToIndex(
            id=str(uuid4()),
            user_id=user_id,
            text=text,
            metadata=metadata,
        )
        self._validate_document(document)
        self._ingestion_queue.enqueue(document)

        return EnqueuedDocument(
            id=document.id,
            queue_name=self._ingestion_queue.queue_name,
            collection_name=self._settings.collection_name,
            model_name=self._settings.model_name,
        )

    def index_document(self, document: DocumentToIndex) -> IndexedDocument:
        self._validate_document(document)
        return self._repository.index_document(document)

    def search(self, search: SearchDocuments) -> list[SearchResult]:
        self._validate_user_id(search.user_id)
        self._validate_query(search.query)
        self._validate_limit(search.limit)
        return self._repository.search(search)

    def _validate_document(self, document: DocumentToIndex) -> None:
        self._validate_document_id(document.id)
        self._validate_user_id(document.user_id)
        self._validate_document_text(document.text)
        self._validate_metadata(document.metadata)

    def _validate_document_id(self, document_id: str) -> None:
        if not document_id.strip():
            raise InvalidDocumentError("Document id is required.")

    def _validate_user_id(self, user_id: str) -> None:
        if not user_id.strip():
            raise InvalidUserError("X-User-Id header is required.")

        if len(user_id) > self._settings.max_user_id_chars:
            raise InvalidUserError(
                f"X-User-Id must have at most "
                f"{self._settings.max_user_id_chars} characters."
            )

    def _validate_document_text(self, text: str) -> None:
        if not text.strip():
            raise InvalidDocumentError("Document text is required.")

        if len(text) > self._settings.max_document_chars:
            raise DocumentTooLargeError(
                f"Document text must have at most "
                f"{self._settings.max_document_chars} characters."
            )

    def _validate_query(self, query: str) -> None:
        if not query.strip():
            raise InvalidDocumentError("Query is required.")

        if len(query) > self._settings.max_query_chars:
            raise QueryTooLargeError(
                f"Query must have at most {self._settings.max_query_chars} "
                "characters."
            )

    def _validate_metadata(self, metadata: dict) -> None:
        if not isinstance(metadata, dict):
            raise InvalidDocumentError("Metadata must be an object.")

        metadata_size = len(
            json.dumps(metadata, ensure_ascii=False).encode("utf-8")
        )
        if metadata_size > self._settings.max_metadata_bytes:
            raise MetadataTooLargeError(
                f"Metadata must have at most "
                f"{self._settings.max_metadata_bytes} bytes."
            )

    def _validate_limit(self, limit: int) -> None:
        if limit > self._settings.max_search_limit:
            raise InvalidLimitError(
                f"Search limit must be at most "
                f"{self._settings.max_search_limit}."
            )
