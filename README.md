# Event API

API REST de Event Sales con el agente de IA HampCode (Spring Boot 4, Spring AI 2, PostgreSQL + pgvector).

Elaborado por Henry Antonio Mendoza Puerta by hampcode. Proyecto académico.

## Ejecutar en local

1. Copia `.env.example` a `.env` y completa los valores.
2. `mvn spring-boot:run` (Java 21). La API queda en `http://localhost:8080/api`.

## Desplegar en Render

El archivo `render.yaml` define el servicio (Docker, plan gratuito). Las claves se piden al crear el Blueprint y nunca se guardan en el repositorio.

La guía del módulo del agente está en `AGENTE.md`.
