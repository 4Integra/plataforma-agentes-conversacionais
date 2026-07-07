# Setup Local

## llm-gateway

No diretório do `llm-gateway`, execute:

```bash
./setup.sh
```

## tool-registry

No diretório do `tool-registry`, execute:

```bash
docker compose up
```

Em outro terminal, execute:

```bash
./gradlew bootRun
```

## memory-service

No diretório do `memory-service`, execute:

```bash
docker compose up
```

Depois, gere o pacote:

```bash
mvn package
```

Execute a aplicação:

```bash
java -jar memory-service-0.1.0-SNAPSHOT.jar
```

## retrieval-service

No diretório do `retrieval-service`, execute:

```bash
docker compose up
```

## name-server

No diretório do `name-server`, execute:

```bash
./gradlew bootRun
```

## agent-service

No diretório do `agent-service`, execute:

```bash
docker compose up
```

## API-Gateway

No diretório do `API-Gateway`, execute:

```bash
./gradlew bootRun
```

## Erro comum

Caso dê erro de `unable to delete directory`, delete manualmente a folder `build`.
