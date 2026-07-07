# memory-service

Armazena e recupera memória de conversas para os agentes da plataforma. Mantém duas camadas de memória:

- **Curto prazo (Redis):** mensagens recentes com TTL, otimizadas para injeção em prompts
- **Longo prazo (PostgreSQL):** histórico completo e durável, com paginação por cursor

Redis é tratado como otimização — se estiver indisponível, writes persistem no PostgreSQL normalmente e reads fazem fallback para o banco.

- **Stack:** Java 21 · Spring Boot 3.3.5 · Spring Data JPA · Spring Data Redis · Flyway
- **Porta padrão:** `8083`
- **Banco de dados:** PostgreSQL (porta padrão `5433`)
- **Cache:** Redis (porta padrão `6379`)

---

## Endpoints

Base path: `/api/v1/conversations`

### `POST /api/v1/conversations`

Cria uma nova conversa. Retorna `HTTP 201` com header `Location`.

**Request body:**
```json
{
  "userId": "user-123",
  "agentId": "agent-service",
  "title": "Sessão de suporte",
  "metadata": { "channel": "web" }
}
```

| Campo      | Tipo   | Obrigatório | Validação            |
|------------|--------|-------------|----------------------|
| `userId`   | string | sim         | máx. 128 caracteres  |
| `agentId`  | string | não         | máx. 128 chars. Default: `default-agent` |
| `title`    | string | não         | máx. 200 caracteres  |
| `metadata` | object | não         | JSON livre           |

**Response body (`ConversationResponse`):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "agentId": "agent-service",
  "title": "Sessão de suporte",
  "metadata": { "channel": "web" },
  "createdAt": "2026-07-06T12:00:00Z",
  "updatedAt": "2026-07-06T12:00:00Z"
}
```

---

### `GET /api/v1/conversations/{conversationId}`

Retorna metadados de uma conversa existente. `404` se não encontrada.

**Response body:** `ConversationResponse` (mesmo formato acima).

---

### `POST /api/v1/conversations/{conversationId}/messages`

Adiciona uma mensagem ao histórico. Persiste no PostgreSQL e, após commit, atualiza o cache Redis. Retorna `HTTP 201`.

**Request body:**
```json
{
  "role": "USER",
  "content": "O que você lembra da conversa?",
  "model": "qwen2.5",
  "toolName": null,
  "correlationId": "req-001",
  "promptTokens": 42,
  "completionTokens": null,
  "metadata": {}
}
```

| Campo              | Tipo    | Obrigatório | Validação                       |
|--------------------|---------|-------------|---------------------------------|
| `role`             | enum    | sim         | `SYSTEM`, `USER`, `ASSISTANT`, `TOOL` |
| `content`          | string  | sim         | máx. 200.000 caracteres         |
| `model`            | string  | não         | máx. 128 caracteres             |
| `toolName`         | string  | não         | máx. 128 caracteres             |
| `correlationId`    | string  | não         | máx. 128 caracteres             |
| `promptTokens`     | integer | não         | ≥ 0                             |
| `completionTokens` | integer | não         | ≥ 0                             |
| `metadata`         | object  | não         | JSON livre                      |

**Response body (`MessageResponse`):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "USER",
  "content": "O que você lembra da conversa?",
  "model": null,
  "toolName": null,
  "correlationId": "req-001",
  "promptTokens": 42,
  "completionTokens": null,
  "metadata": {},
  "createdAt": "2026-07-06T12:00:01Z"
}
```

---

### `GET /api/v1/conversations/{conversationId}/messages`

Histórico completo com paginação por cursor (do mais novo para o mais antigo).

| Query param | Default | Descrição                                    |
|-------------|---------|----------------------------------------------|
| `limit`     | `50`    | Quantidade de mensagens (mín. 1, máx. `MEMORY_MAX_PAGE_SIZE`) |
| `before`    | —       | Cursor: retorna mensagens anteriores a este `Instant` |

**Response body (`MessageHistoryResponse`):**
```json
{
  "conversationId": "550e8400-...",
  "messages": [ ... ],
  "limit": 50,
  "before": null,
  "nextBefore": "2026-07-06T11:59:00Z",
  "hasMore": true
}
```

Para paginar: use o valor de `nextBefore` como `before` na próxima requisição.

---

### `GET /api/v1/conversations/{conversationId}/memory`

Retorna as mensagens recentes em ordem cronológica, prontas para injeção em um prompt. Prioriza o cache Redis; se vazio, busca no PostgreSQL e recarrega o cache.

| Query param | Default | Descrição                  |
|-------------|---------|----------------------------|
| `limit`     | `20`    | Quantidade de mensagens (mín. 1) |

**Response body (`MemoryContextResponse`):**
```json
{
  "conversationId": "550e8400-...",
  "messages": [ ... ],
  "source": "redis",
  "limit": 20,
  "generatedAt": "2026-07-06T12:00:05Z"
}
```

O campo `source` indica a origem dos dados: `"redis"` (cache hit) ou `"postgres"` (fallback).

---

### `DELETE /api/v1/conversations/{conversationId}/memory/cache`

Invalida o cache Redis de curto prazo de uma conversa. Útil para forçar recarga após operações externas. Retorna `HTTP 204`.

---

## Erros

Todos os erros seguem o formato `ApiError`:

```json
{
  "timestamp": "2026-07-06T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conversation 550e8400-... not found",
  "path": "/api/v1/conversations/550e8400-...",
  "fieldErrors": {}
}
```

| Status | Causa                                                     |
|--------|-----------------------------------------------------------|
| `400`  | Validação de campos, body malformado, `limit < 1`         |
| `404`  | Conversa não encontrada                                   |

---

## Arquitetura interna

```
ConversationMemoryController (REST)
        │
        ▼
ConversationMemoryService (orquestrador, @Transactional)
    ├── ConversationMemoryStorage ──► ConversationMemoryJpaStorage (PostgreSQL)
    └── ShortTermMemoryStore     ──► RedisShortTermMemoryStore (Redis)
```

O update do Redis é sempre feito **após o commit da transação** (`afterCommit`) para evitar inconsistência entre os dois stores.

### Estratégia do cache Redis

- **Append:** `RPUSH` + `LTRIM` para manter no máximo `maxMessages` entradas + `EXPIRE` com TTL configurável
- **Read:** `LRANGE` das últimas `limit` entradas
- **Miss:** lê do PostgreSQL em ordem reversa, reverte para cronológica e repõe o cache
- **Chave:** `memory:conversation:{uuid}:recent` (prefixo configurável)
- **Falhas de Redis** são silenciosas — apenas logadas como `WARN`, sem impactar o fluxo principal

---

## Schema do banco (PostgreSQL)

Gerenciado por Flyway (`V1__init_memory_schema.sql`).

**Tabela `conversations`**

| Coluna       | Tipo           | Detalhe                                  |
|--------------|----------------|------------------------------------------|
| `id`         | uuid PK        |                                          |
| `user_id`    | varchar(128)   | NOT NULL                                 |
| `agent_id`   | varchar(128)   | NOT NULL                                 |
| `title`      | varchar(200)   | opcional                                 |
| `metadata`   | jsonb          | default `{}`                             |
| `created_at` | timestamptz    | NOT NULL                                 |
| `updated_at` | timestamptz    | NOT NULL, atualizado a cada mensagem     |
| `version`    | bigint         | controle de concorrência otimista (JPA)  |

Índice: `(user_id, agent_id, updated_at DESC)`

**Tabela `conversation_messages`**

| Coluna              | Tipo         | Detalhe                                        |
|---------------------|--------------|------------------------------------------------|
| `id`                | uuid PK      |                                                |
| `conversation_id`   | uuid FK      | ON DELETE CASCADE                              |
| `role`              | varchar(32)  | CHECK: `SYSTEM`, `USER`, `ASSISTANT`, `TOOL`   |
| `content`           | text         | NOT NULL                                       |
| `model`             | varchar(128) | opcional                                       |
| `tool_name`         | varchar(128) | opcional                                       |
| `correlation_id`    | varchar(128) | opcional, indexado (WHERE NOT NULL)            |
| `prompt_tokens`     | integer      | CHECK ≥ 0                                      |
| `completion_tokens` | integer      | CHECK ≥ 0                                      |
| `metadata`          | jsonb        | default `{}`                                   |
| `created_at`        | timestamptz  | NOT NULL, imutável                             |

Índices: `(conversation_id, created_at DESC, id DESC)` e `(correlation_id) WHERE NOT NULL`

---

## Configuração

| Propriedade                          | Env var                        | Padrão                              |
|--------------------------------------|--------------------------------|-------------------------------------|
| `server.port`                        | `SERVER_PORT`                  | `8083`                              |
| `spring.datasource.url`              | `SPRING_DATASOURCE_URL`        | `jdbc:postgresql://localhost:5433/memory_service` |
| `spring.datasource.username`         | `SPRING_DATASOURCE_USERNAME`   | `memory`                            |
| `spring.datasource.password`         | `SPRING_DATASOURCE_PASSWORD`   | `memory`                            |
| `spring.datasource.hikari.maximum-pool-size` | `DB_POOL_MAX_SIZE`   | `10`                                |
| `spring.datasource.hikari.minimum-idle` | `DB_POOL_MIN_IDLE`        | `2`                                 |
| `spring.data.redis.host`             | `REDIS_HOST`                   | `localhost`                         |
| `spring.data.redis.port`             | `REDIS_PORT`                   | `6379`                              |
| `spring.data.redis.timeout`          | `REDIS_TIMEOUT`                | `2s`                                |
| `memory.max-page-size`               | `MEMORY_MAX_PAGE_SIZE`         | `200`                               |
| `memory.short-term.enabled`          | `MEMORY_SHORT_TERM_ENABLED`    | `true`                              |
| `memory.short-term.max-messages`     | `MEMORY_SHORT_TERM_MAX_MESSAGES` | `50`                              |
| `memory.short-term.ttl`              | `MEMORY_SHORT_TERM_TTL`        | `PT12H` (12 horas)                  |
| `memory.short-term.key-prefix`       | `MEMORY_SHORT_TERM_KEY_PREFIX` | `memory:conversation:`              |

---

## Endpoints de operação (Actuator)

| Endpoint                       | Descrição                            |
|--------------------------------|--------------------------------------|
| `GET /actuator/health`         | Liveness / readiness (probes K8s)    |
| `GET /actuator/health/liveness` | Liveness probe                      |
| `GET /actuator/health/readiness`| Readiness probe                     |
| `GET /actuator/metrics`        | Métricas Micrometer                  |
| `GET /actuator/prometheus`     | Métricas no formato Prometheus       |

---

## Build e Docker

```bash
# Testes
mvn test

# Build do JAR
mvn package

# Build da imagem (multi-stage: build + JRE Alpine)
docker build -t memory-service .

# Rodar o container
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5433/memory_service \
  -e SPRING_DATASOURCE_USERNAME=memory \
  -e SPRING_DATASOURCE_PASSWORD=memory \
  -e REDIS_HOST=redis \
  memory-service
```

A imagem usa build multi-stage: `maven:3.9.9-eclipse-temurin-21` para compilar e `eclipse-temurin:21-jre-alpine` para rodar. O processo roda com usuário não-root (`app`).

---

## Executando localmente

**Pré-requisitos:** Java 21+, PostgreSQL e Redis rodando.

```bash
mvn spring-boot:run
```

Com variáveis customizadas:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/memory_service \
SPRING_DATASOURCE_USERNAME=memory \
SPRING_DATASOURCE_PASSWORD=memory \
REDIS_HOST=localhost \
mvn spring-boot:run
```

---

## Testando

```bash
# Criar conversa
curl -X POST http://localhost:8083/api/v1/conversations \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123", "agentId": "agent-service"}'

# Adicionar mensagem
curl -X POST http://localhost:8083/api/v1/conversations/{id}/messages \
  -H "Content-Type: application/json" \
  -d '{"role": "USER", "content": "Olá!"}'

# Recuperar memória de curto prazo (para injetar em prompt)
curl "http://localhost:8083/api/v1/conversations/{id}/memory?limit=20"

# Histórico completo com paginação
curl "http://localhost:8083/api/v1/conversations/{id}/messages?limit=50"

# Próxima página (cursor)
curl "http://localhost:8083/api/v1/conversations/{id}/messages?limit=50&before=2026-07-06T12:00:00Z"

# Invalidar cache Redis
curl -X DELETE http://localhost:8083/api/v1/conversations/{id}/memory/cache
```

**Swagger UI** (quando rodando):
```
http://localhost:8083/swagger-ui/index.html
```

---

## Referências

- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Flyway — Database Migrations](https://documentation.red-gate.com/fd)
- [Micrometer — Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.3.5/reference/html/actuator.html)
