# memory-service

Stores and retrieves conversation memory for conversational agents.

## Design

The service keeps two memory layers:

- Short-term memory in Redis: bounded recent messages with TTL, optimized for prompt context retrieval.
- Long-term memory in PostgreSQL: complete durable history, indexed by conversation and creation time.

Redis is treated as an optimization. If Redis is unavailable, writes still commit to PostgreSQL and reads fall back to durable history.

## API

### Create conversation

`POST /api/v1/conversations`

```json
{
  "userId": "user-123",
  "agentId": "agent-main",
  "title": "Support chat",
  "metadata": {
    "tenantId": "local"
  }
}
```

### Append message

`POST /api/v1/conversations/{conversationId}/messages`

```json
{
  "role": "USER",
  "content": "O que voce lembra da conversa?",
  "model": "llama3.1",
  "correlationId": "request-001",
  "metadata": {
    "channel": "web"
  }
}
```

### Retrieve short-term memory

`GET /api/v1/conversations/{conversationId}/memory?limit=20`

Returns messages in chronological order, ready to be injected into an agent prompt.

### Retrieve long-term history

`GET /api/v1/conversations/{conversationId}/messages?limit=50&before=2026-07-06T12:00:00Z`

Returns messages newest first, with `nextBefore` for cursor pagination.

## Configuration

Environment variables:

- `SERVER_PORT`, default `8083`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `REDIS_HOST`, default `localhost`
- `REDIS_PORT`, default `6379`
- `MEMORY_SHORT_TERM_TTL`, default `PT12H`
- `MEMORY_SHORT_TERM_MAX_MESSAGES`, default `50`

## Build

```bash
mvn test
mvn package
```

