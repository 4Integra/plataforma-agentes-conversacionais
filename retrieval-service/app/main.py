from fastapi import FastAPI
from qdrant_client import QdrantClient

from app.api import router
from app.core.config import get_settings
from app.api.error_handlers import register_error_handlers
from app.api.middleware import register_middlewares
from app.application.retrieval_service import RetrievalService
from app.infrastructure.qdrant.retrieval_repository import QdrantRetrievalRepository
from app.infrastructure.rabbitmq.ingestion_consumer import RabbitMQIngestionConsumer
from app.infrastructure.rabbitmq.ingestion_queue import RabbitMQIngestionQueuePublisher


def create_app() -> FastAPI:
    settings = get_settings()

    fastapi_app = FastAPI(title="Retrieval Service")
    register_middlewares(fastapi_app, settings)
    register_error_handlers(fastapi_app)

    qdrant = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
    repository = QdrantRetrievalRepository(
        client=qdrant,
        collection_name=settings.collection_name,
        model_name=settings.model_name,
        model_vector_size=settings.model_vector_size,
    )
    ingestion_queue = RabbitMQIngestionQueuePublisher(settings=settings)
    fastapi_app.state.retrieval_service = RetrievalService(
        settings=settings,
        repository=repository,
        ingestion_queue=ingestion_queue,
    )
    fastapi_app.state.ingestion_consumer = RabbitMQIngestionConsumer(
        settings=settings,
        retrieval_service=fastapi_app.state.retrieval_service,
    )

    @fastapi_app.on_event("startup")
    def startup() -> None:
        fastapi_app.state.retrieval_service.ensure_ready()
        if settings.ingestion_consumer_enabled:
            fastapi_app.state.ingestion_consumer.start()

    @fastapi_app.on_event("shutdown")
    def shutdown() -> None:
        if settings.ingestion_consumer_enabled:
            fastapi_app.state.ingestion_consumer.stop()

    fastapi_app.include_router(router)
    return fastapi_app


app = create_app()
