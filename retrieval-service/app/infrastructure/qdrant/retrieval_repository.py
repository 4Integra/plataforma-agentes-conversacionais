from threading import Lock

from qdrant_client import QdrantClient, models

from app.domain.entities import DocumentToIndex, IndexedDocument, SearchDocuments
from app.domain.entities import SearchResult
from app.domain.exceptions import CollectionConfigurationError


class QdrantRetrievalRepository:
    def __init__(
        self,
        client: QdrantClient,
        collection_name: str,
        model_name: str,
        model_vector_size: int,
    ) -> None:
        self._client = client
        self._collection_name = collection_name
        self._model_name = model_name
        self._model_vector_size = model_vector_size
        self._embedding_lock = Lock()

    def ensure_collection(self) -> None:
        if not self._collection_exists():
            self._client.create_collection(
                collection_name=self._collection_name,
                vectors_config=models.VectorParams(
                    size=self._model_vector_size,
                    distance=models.Distance.COSINE,
                ),
            )
            return

        collection_vector_size = self._get_collection_vector_size()
        if collection_vector_size != self._model_vector_size:
            raise CollectionConfigurationError(
                f"Collection '{self._collection_name}' has vector size "
                f"{collection_vector_size}, but model '{self._model_name}' "
                f"requires vector size {self._model_vector_size}."
            )

    def index_document(self, document: DocumentToIndex) -> IndexedDocument:
        payload = {
            "document_id": document.id,
            "user_id": document.user_id,
            "text": document.text,
            "metadata": document.metadata,
            "embedding_model": self._model_name,
        }

        with self._embedding_lock:
            self._client.upload_collection(
                collection_name=self._collection_name,
                vectors=[
                    models.Document(
                        text=document.text,
                        model=self._model_name,
                    )
                ],
                payload=[payload],
                ids=[document.id],
            )

        return IndexedDocument(
            id=document.id,
            collection_name=self._collection_name,
            model_name=self._model_name,
        )

    def search(self, search: SearchDocuments) -> list[SearchResult]:
        with self._embedding_lock:
            points = self._client.query_points(
                collection_name=self._collection_name,
                query=models.Document(
                    text=search.query,
                    model=self._model_name,
                ),
                query_filter=self._user_filter(search.user_id),
                limit=search.limit,
                with_payload=True,
            ).points

        return [
            SearchResult(
                score=point.score,
                text=(point.payload or {}).get("text"),
                metadata=(point.payload or {}).get("metadata", {}),
                model_name=(point.payload or {}).get("embedding_model"),
            )
            for point in points
        ]

    def _collection_exists(self) -> bool:
        collections = self._client.get_collections().collections
        return any(
            collection.name == self._collection_name
            for collection in collections
        )

    def _get_collection_vector_size(self) -> int:
        collection_info = self._client.get_collection(
            collection_name=self._collection_name
        )
        vectors_config = collection_info.config.params.vectors

        if hasattr(vectors_config, "size"):
            return vectors_config.size

        raise CollectionConfigurationError(
            f"Collection '{self._collection_name}' must use a single-vector "
            "configuration."
        )

    def _user_filter(self, user_id: str) -> models.Filter:
        return models.Filter(
            must=[
                models.FieldCondition(
                    key="user_id",
                    match=models.MatchValue(value=user_id),
                )
            ]
        )
