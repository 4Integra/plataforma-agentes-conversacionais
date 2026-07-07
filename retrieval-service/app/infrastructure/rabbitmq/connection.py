import pika

from app.core.config import Settings


def build_connection_parameters(settings: Settings) -> pika.ConnectionParameters:
    credentials = pika.PlainCredentials(
        username=settings.rabbitmq_username,
        password=settings.rabbitmq_password,
    )

    return pika.ConnectionParameters(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        virtual_host=settings.rabbitmq_virtual_host,
        credentials=credentials,
        heartbeat=30,
        blocked_connection_timeout=30,
    )
