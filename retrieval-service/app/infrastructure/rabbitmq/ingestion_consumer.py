import json
import logging
import threading
import time
from typing import Any

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import AMQPConnectionError

from app.core.config import Settings
from app.domain.entities import DocumentToIndex
from app.domain.exceptions import InvalidDocumentError, RetrievalError
from app.infrastructure.rabbitmq.connection import build_connection_parameters
from app.application.retrieval_service import RetrievalService

logger = logging.getLogger(__name__)


class RabbitMQIngestionConsumer:
    def __init__(
        self,
        settings: Settings,
        retrieval_service: RetrievalService,
    ) -> None:
        self._settings = settings
        self._retrieval_service = retrieval_service
        self._connection_parameters = build_connection_parameters(settings)
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None
        self._connection: pika.BlockingConnection | None = None

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return

        self._thread = threading.Thread(
            target=self._run,
            name="rabbitmq-ingestion-consumer",
            daemon=True,
        )
        self._thread.start()

    def stop(self) -> None:
        self._stop_event.set()
        if self._connection and self._connection.is_open:
            self._connection.add_callback_threadsafe(self._connection.close)

        if self._thread:
            self._thread.join(timeout=10)

    def _run(self) -> None:
        while not self._stop_event.is_set():
            try:
                self._consume()
            except AMQPConnectionError:
                logger.exception("RabbitMQ connection failed.")
                time.sleep(self._settings.ingestion_retry_delay_seconds)
            except Exception:
                logger.exception("RabbitMQ consumer failed.")
                time.sleep(self._settings.ingestion_retry_delay_seconds)

    def _consume(self) -> None:
        self._connection = pika.BlockingConnection(self._connection_parameters)
        channel = self._connection.channel()
        channel.queue_declare(
            queue=self._settings.document_ingestion_queue,
            durable=True,
        )
        channel.basic_qos(prefetch_count=self._settings.ingestion_prefetch_count)
        channel.basic_consume(
            queue=self._settings.document_ingestion_queue,
            on_message_callback=self._handle_message,
        )
        channel.start_consuming()

    def _handle_message(
        self,
        channel: BlockingChannel,
        method: Any,
        _properties: pika.BasicProperties,
        body: bytes,
    ) -> None:
        try:
            document = self._decode_document(body)
            self._retrieval_service.index_document(document)
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except RetrievalError:
            logger.exception("Invalid ingestion message discarded.")
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception:
            logger.exception("Document indexing failed; message requeued.")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def _decode_document(self, body: bytes) -> DocumentToIndex:
        try:
            payload: dict[str, Any] = json.loads(body.decode("utf-8"))
            return DocumentToIndex(
                id=str(payload["id"]),
                user_id=str(payload["user_id"]),
                text=str(payload["text"]),
                metadata=payload.get("metadata", {}),
            )
        except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
            raise InvalidDocumentError("Invalid ingestion message.") from exc
