# Retrieval Service

Servico de busca semantica com FastAPI, Qdrant, FastEmbed e RabbitMQ.

## Requisitos

- Docker
- Docker Compose

## Como executar

Entre na pasta do servico:

```bash
cd retrieval-service
```

Suba a aplicacao:

```bash
docker compose up --build -d
```

A API ficara disponivel em:

```text
http://localhost:8000
```

O painel do RabbitMQ ficara em:

```text
http://localhost:15672
```

Usuario e senha:

```text
guest / guest
```

## Testar se esta funcionando

Health check:

```bash
curl http://localhost:8000/health
```

Resposta esperada:

```json
{"status":"ok"}
```

## Enfileirar um documento

```bash
curl -X POST http://localhost:8000/documents \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-123" \
  -d "{\"text\":\"Qdrant armazena embeddings para busca semantica.\",\"metadata\":{\"source\":\"teste\"}}"
```

Resposta esperada:

```json
{
  "status": "queued",
  "id": "document-id",
  "queue_name": "retrieval.document-ingestion",
  "collection_name": "documents",
  "model_name": "BAAI/bge-small-en-v1.5"
}
```

O documento sera processado pelo consumidor RabbitMQ e indexado no Qdrant.

## Buscar documentos

Aguarde alguns segundos apos enfileirar o documento e execute:

```bash
curl -X POST http://localhost:8000/search \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-123" \
  -d "{\"query\":\"Onde embeddings sao armazenados?\",\"limit\":5}"
```

## Parar a aplicacao

```bash
docker compose down
```

## Configuracoes principais

As principais variaveis ficam no `docker-compose.yaml`:

- `COLLECTION_NAME`: collection usada no Qdrant.
- `MODEL_NAME`: modelo de embedding usado.
- `ALLOWED_MODEL_NAMES`: modelos permitidos.
- `DOCUMENT_INGESTION_QUEUE`: fila de ingestao no RabbitMQ.
- `INGESTION_PREFETCH_COUNT`: quantidade de mensagens consumidas por vez.
- `MAX_DOCUMENT_CHARS`: tamanho maximo do texto do documento.
