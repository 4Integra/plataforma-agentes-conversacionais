import unittest

from app.core.config import Settings
from app.domain.entities import DocumentToIndex, IndexedDocument, SearchDocuments
from app.domain.entities import SearchResult
from app.domain.exceptions import DocumentTooLargeError, InvalidLimitError
from app.domain.exceptions import InvalidUserError
from app.application.retrieval_service import RetrievalService


def make_settings(**overrides):
    values = {
        "collection_name": "documents",
        "model_name": "BAAI/bge-small-en-v1.5",
        "allowed_model_names": ("BAAI/bge-small-en-v1.5",),
        "qdrant_host": "qdrant",
        "qdrant_port": 6333,
        "max_document_chars": 100,
        "max_query_chars": 50,
        "max_metadata_bytes": 1024,
        "max_request_bytes": 4096,
        "max_search_limit": 5,
        "max_user_id_chars": 20,
        "rabbitmq_host": "rabbitmq",
        "rabbitmq_port": 5672,
        "rabbitmq_username": "guest",
        "rabbitmq_password": "guest",
        "rabbitmq_virtual_host": "/",
        "document_ingestion_queue": "retrieval.document-ingestion",
        "ingestion_prefetch_count": 1,
        "ingestion_consumer_enabled": True,
        "ingestion_retry_delay_seconds": 1,
    }
    values.update(overrides)
    return Settings(**values)


class FakeRepository:
    def __init__(self):
        self.indexed_documents = []
        self.searches = []
        self.collection_ready = False

    def ensure_collection(self):
        self.collection_ready = True

    def index_document(self, document):
        self.indexed_documents.append(document)
        return IndexedDocument(
            id=document.id,
            collection_name="documents",
            model_name="BAAI/bge-small-en-v1.5",
        )

    def search(self, search):
        self.searches.append(search)
        return [
            SearchResult(
                score=0.99,
                text="matched text",
                metadata={"source": "test"},
                model_name="BAAI/bge-small-en-v1.5",
            )
        ]


class FakeIngestionQueue:
    queue_name = "retrieval.document-ingestion"

    def __init__(self):
        self.documents = []

    def enqueue(self, document):
        self.documents.append(document)


class RetrievalServiceTest(unittest.TestCase):
    def make_service(self, **settings_overrides):
        repository = FakeRepository()
        queue = FakeIngestionQueue()
        service = RetrievalService(
            settings=make_settings(**settings_overrides),
            repository=repository,
            ingestion_queue=queue,
        )
        return service, repository, queue

    def test_enqueue_document_publishes_valid_message(self):
        service, _repository, queue = self.make_service()

        result = service.enqueue_document(
            user_id="user-1",
            text="document text",
            metadata={"source": "unit-test"},
        )

        self.assertEqual(result.queue_name, "retrieval.document-ingestion")
        self.assertEqual(result.collection_name, "documents")
        self.assertEqual(result.model_name, "BAAI/bge-small-en-v1.5")
        self.assertEqual(len(queue.documents), 1)
        self.assertEqual(queue.documents[0].id, result.id)
        self.assertEqual(queue.documents[0].user_id, "user-1")

    def test_document_too_large_is_rejected_before_queueing(self):
        service, _repository, queue = self.make_service(max_document_chars=3)

        with self.assertRaises(DocumentTooLargeError):
            service.enqueue_document(user_id="user-1", text="abcd", metadata={})

        self.assertEqual(queue.documents, [])

    def test_missing_user_is_rejected_before_queueing(self):
        service, _repository, queue = self.make_service()

        with self.assertRaises(InvalidUserError):
            service.enqueue_document(user_id=" ", text="document", metadata={})

        self.assertEqual(queue.documents, [])

    def test_consumer_indexing_keeps_document_id_stable(self):
        service, repository, _queue = self.make_service()
        document = DocumentToIndex(
            id="doc-123",
            user_id="user-1",
            text="document text",
            metadata={},
        )

        result = service.index_document(document)

        self.assertEqual(result.id, "doc-123")
        self.assertEqual(repository.indexed_documents[0].id, "doc-123")

    def test_search_limit_is_validated_before_repository_call(self):
        service, repository, _queue = self.make_service(max_search_limit=2)

        with self.assertRaises(InvalidLimitError):
            service.search(SearchDocuments(user_id="user-1", query="test", limit=3))

        self.assertEqual(repository.searches, [])

    def test_search_passes_user_id_to_repository_for_isolation(self):
        service, repository, _queue = self.make_service()

        results = service.search(
            SearchDocuments(user_id="user-1", query="test", limit=1)
        )

        self.assertEqual(len(results), 1)
        self.assertEqual(repository.searches[0].user_id, "user-1")


if __name__ == "__main__":
    unittest.main()
