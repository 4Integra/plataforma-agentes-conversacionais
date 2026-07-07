from typing import Protocol

from app.domain.entities import DocumentToIndex, IndexedDocument, SearchDocuments
from app.domain.entities import SearchResult


class RetrievalRepository(Protocol):
    def ensure_collection(self) -> None:
        ...

    def index_document(self, document: DocumentToIndex) -> IndexedDocument:
        ...

    def search(self, search: SearchDocuments) -> list[SearchResult]:
        ...


class IngestionQueuePublisher(Protocol):
    @property
    def queue_name(self) -> str:
        ...

    def enqueue(self, document: DocumentToIndex) -> None:
        ...
