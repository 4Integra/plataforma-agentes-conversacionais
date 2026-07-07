from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class DocumentToIndex:
    id: str
    user_id: str
    text: str
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class SearchDocuments:
    user_id: str
    query: str
    limit: int


@dataclass(frozen=True)
class IndexedDocument:
    id: str
    collection_name: str
    model_name: str


@dataclass(frozen=True)
class EnqueuedDocument:
    id: str
    queue_name: str
    collection_name: str
    model_name: str


@dataclass(frozen=True)
class SearchResult:
    score: float
    text: str | None
    metadata: dict[str, Any]
    model_name: str | None
