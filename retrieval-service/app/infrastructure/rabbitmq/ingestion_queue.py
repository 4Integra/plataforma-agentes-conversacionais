import json

import pika
from pika.exceptions import AMQPError

from app.core.config import Settings
from app.domain.entities import DocumentToIndex
from app.domain.exceptions import IngestionQueueUnavailableError
from app.domain.ports import IngestionQueuePublisher
from app.infrastructure.rabbitmq.connection import build_connection_parameters


class RabbitMQIngestionQueuePublisher(IngestionQueuePublisher):
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._connection_parameters = build_connection_parameters(settings)

    @property
    def queue_name(self) -> str:
        return self._settings.document_ingestion_queue

    def enqueue(self, document: DocumentToIndex) -> None:
        message = {
            "id": document.id,
            "user_id": document.user_id,
            "text": document.text,
            "metadata": document.metadata,
        }

        try:
            with pika.BlockingConnection(self._connection_parameters) as connection:
                channel = connection.channel()
                channel.queue_declare(queue=self.queue_name, durable=True)
                channel.basic_publish(
                    exchange="",
                    routing_key=self.queue_name,
                    body=json.dumps(message, ensure_ascii=False).encode("utf-8"),
                    properties=pika.BasicProperties(
                        content_type="application/json",
                        delivery_mode=2,
                    ),
                )
        except AMQPError as exc:
            raise IngestionQueueUnavailableError(
                "Document ingestion queue is unavailable."
            ) from exc
