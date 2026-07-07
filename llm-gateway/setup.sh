#!/bin/bash

docker compose up -d

echo "Aguardando Ollama iniciar..."
sleep 10

echo "Baixando modelo qwen2.5..."
docker exec -it ollama ollama pull qwen2.5

echo "Setup concluído!"
