# Módulo de agente (Spring AI)

Asistente de chat con el patrón **Routing**: un router elige el agente según el mensaje y ese agente responde usando sus tools.

```
mensaje ─► RoutingWorkflow ─► agente elegido ─► tools ─► ResponseData ─► AgentResponse
           (elige por           (prompt .md +     (leen la BD       (tarjetas,       (JSON para
            descripción)         sus tools)        del proyecto)     gráficos)        el frontend)
```

## Estructura

```
agent/
├─ core/                    MOTOR REUTILIZABLE: no se modifica
│  ├─ AgentService          orquesta: router, prompt, tools, memoria y PDFs
│  ├─ AgentController       /agent/chat y conversaciones
│  ├─ AgentDefinition       cómo se describe un agente
│  ├─ routing/              RoutingWorkflow, RouterDecision
│  ├─ conversation/         panel de conversaciones y memoria
│  ├─ knowledge/            RAG: KnowledgeLoader (ETL), KnowledgeConfig, KnowledgeController
│  └─ voice/                VoiceService: transcribir audio y generar voz
└─ app/                     LO QUE CADA EQUIPO CAMBIA
   ├─ AgentCatalog          la lista de agentes (un método por agente)
   ├─ tools/                las tools: una clase por agente
   ├─ dto/                  lo que se envía al frontend
   ├─ PurchaseController    botones Confirmar / No (propio de la compra)
   └─ VoiceController       /agent/voice: audio → texto → agente → respuesta (y voz)

resources/
├─ prompts/                 common.md + un .md por agente (mismo nombre que el agente)
└─ knowledge/               PDFs para el agente de políticas
```

## Agentes de este proyecto

| Agente | Rol | Tools | Qué hace |
|---|---|---|---|
| `events` | todos | `searchEvents` | consulta eventos, cupos y precios |
| `purchase` | PARTICIPANT | `buy`, `confirm` | compra en dos pasos: pendiente y confirmar |
| `info` | todos | ninguna | responde políticas con los PDFs |
| `report` | ORGANIZER | `myEvents`, `sales`, `exportSales` | eventos propios, ventas con gráfico comparativo y reporte en Excel |

El primer agente del catálogo (`@Order(1)`) es el de respaldo si el router no acierta.
Un usuario sin el rol del agente recibe un mensaje fijo, sin llamar al modelo.

## Integrarlo en otro proyecto

1. **Dependencias** (`pom.xml`): `spring-ai-starter-model-openai`, `spring-ai-starter-model-chat-memory-repository-jdbc`, `spring-ai-starter-vector-store-pgvector`, `spring-ai-vector-store-advisor`, `spring-ai-pdf-document-reader`, JPA y PostgreSQL con pgvector.
2. **Configuración** (`application.yml`): clave y modelo de IA, base vectorial y memoria.
3. **Tablas**: `agent_conversations`, `vector_store` y la de la memoria del chat.
4. **Copiar** `agent/core` y cambiar el nombre base del paquete.
5. **Una línea a cambiar en `core`**: `ConversationService` lanza `ResourceNotFoundException`; usa la excepción 404 de tu proyecto.
6. **Crear tu `app/`** (siguiente sección).

## Qué debe cambiar cada equipo

| Qué | Dónde | Cómo |
|---|---|---|
| Los agentes | `app/AgentCatalog` | un `@Bean AgentDefinition` por agente: nombre, descripción para el router, rol requerido, si usa PDFs y sus tools |
| Las tools | `app/tools/*` | métodos `@Tool` que llaman a los **servicios de tu proyecto** |
| Cómo habla cada agente | `resources/prompts/` | `common.md` (reglas generales, `%s` es la fecha) y un `.md` por agente |
| Lo que ve la pantalla | `app/dto/*` | `ResponseData` y `AgentResponse` con los campos de tu dominio |
| Políticas | `resources/knowledge/*.pdf` | tus propios PDFs |

## Agregar un agente nuevo

1. Escribe `resources/prompts/<nombre>.md`.
2. Crea su clase de tools en `app/tools` (si necesita datos).
3. Agrega un `@Bean AgentDefinition` en `AgentCatalog` con un `@Order` nuevo.

El router lo descubre solo por su `description`: sé específico al redactarla, porque con eso decide.

## RAG: cómo el agente de políticas usa los PDFs

RAG (*Retrieval-Augmented Generation*): antes de responder, el agente busca fragmentos de tus documentos y el modelo responde con ellos. Todo vive en `core/knowledge`.

**1. Al arrancar: el ETL** (`KnowledgeLoader`). Para cada PDF de `resources/knowledge/`:

| Paso | Método | Qué hace |
|---|---|---|
| Comprobar | `isLoaded` | Calcula el hash (SHA-256) del PDF y lo compara con el guardado en la base. Si es igual, no hace nada. |
| Limpiar | `deleteFragmentsOf` | Si el PDF cambió, borra sus fragmentos anteriores. |
| **E**xtract | `extract` | `PagePdfDocumentReader`: una página del PDF es un `Document` con sus metadatos. |
| **T**ransform | `transform` | `TokenTextSplitter` parte cada página en fragmentos (*chunks*) de hasta 800 tokens y se les agrega el hash. |
| **L**oad | `load` | `vectorStore.add`: genera el **embedding** de cada fragmento (`text-embedding-3-small`, 1536 números) y lo guarda en la tabla `vector_store` de pgvector. |

**2. En cada pregunta de políticas** (`knowledgeAdvisor`, definido en `KnowledgeConfig`). Solo se activa en los agentes con `usesPdfs = true`:

1. Convierte la pregunta en un embedding con el mismo modelo.
2. Hace una **búsqueda por similitud** (distancia coseno, índice HNSW): trae los 4 fragmentos más cercanos con similitud mínima de 0.3.
3. Agrega esos fragmentos al prompt como `CONTEXTO`.
4. El modelo responde solo con ese contexto (regla en `prompts/info.md`).

**Conceptos:** un *embedding* es la lista de números que representa el significado de un texto; la *base de datos vectorial* guarda esos vectores; la *búsqueda por similitud* encuentra los más cercanos a la pregunta, y por eso encuentra por significado y no por palabras exactas.

**Ajustes** (constantes en `KnowledgeConfig`): `CHUNK_SIZE_TOKENS`, `TOP_K` y `SIMILARITY_THRESHOLD`. La dimensión del vector (`dimensions` en `application.yml`) debe coincidir con el modelo de embeddings.

**Probar la búsqueda:** `GET /agent/knowledge/search?q=...` devuelve los fragmentos encontrados con su archivo, página y puntaje.

**Cambiar un PDF:** reemplaza el archivo y reinicia. `KnowledgeLoader` detecta el cambio por el hash, borra los fragmentos viejos y carga los nuevos.

## Voz

La voz es otra forma de entrar y salir del mismo chat: **no cambia el agente**.

```
audio ─► VoiceService.transcribe ─► AgentService.chat (el mismo de siempre) ─► respuesta ─► VoiceService.speak (opcional)
```

| Pieza | Dónde | Qué hace |
|---|---|---|
| `VoiceService` | `core/voice` | Transcribe con Whisper (`TranscriptionModel`) y genera voz con `TextToSpeechModel`. No sabe nada del dominio. |
| `VoiceController` | `app/` | `POST /agent/voice` (multipart: `audio`, `conversationId`, `speak`). Devuelve `VoiceResponse`: lo transcrito, el `AgentResponse` de siempre y el audio en base64. |
| Botón **Hablar** | frontend | Graba con `MediaRecorder`, envía el audio y reproduce la respuesta. |

**Configuración** (`application.yml`): `spring.ai.openai.audio.transcription` (`whisper-1`, idioma `es`) y `spring.ai.openai.audio.speech` (`tts-1`, voz `alloy`). Se apaga con `app.voice.enabled=false` (variable `VOICE_ENABLED`).

**Seguridad de las compras:** si el usuario tiene una compra pendiente, la voz no llega al agente y responde que se confirme o cancele con los botones. Así una mala transcripción de "sí" nunca ejecuta una compra.

**Requisitos:** el proveedor debe ofrecer los endpoints de audio de OpenAI, y el navegador exige HTTPS (o `localhost`) para usar el micrófono.

## Estructura de un prompt

`common.md` se antepone al prompt de cada agente. Todos siguen las mismas secciones:

| Archivo | Secciones |
|---|---|
| `common.md` | `# Rol` (quién es el asistente; `%s` es la fecha) y `# Reglas generales` |
| `<agente>.md` | `# Tarea`, `# Tools` (cuándo usar cada una), `# Reglas` y `# Cuando algo falla` |

Sirven para el tono y para indicar qué tool usar. Lo que debe cumplirse siempre (permisos, compra en dos pasos, mensajes con datos) está en el código, no en el prompt.

## Escribir una tool

- Una tool es un método `@Tool` con una `description` clara: el modelo decide con ella cuándo llamarla.
- Recibe el `ToolContext` como último parámetro. De ahí salen `ResponseData.of(context)` (para dejar tarjetas o gráficos) y el usuario (`"user"`).
- Si falla, lanza una excepción con un mensaje claro: el modelo lo recibe y se lo explica al usuario.
- Los datos que deben verse en pantalla se guardan en `ResponseData`; el modelo no los repite.

## Cómo se evita que el agente invente

- Los agentes tienen pocas tools y cada una está en su agente: el modelo no puede elegir mal entre cinco.
- Cuando hay datos, el mensaje lo escribe `ResponseData.message(...)`, no el modelo.
- La compra necesita dos pasos: `buy` deja una compra pendiente y solo `confirm` (o el botón) la ejecuta. `confirm` no hace nada si en el mismo mensaje ya se pidió la compra.
- El agente de políticas responde solo con el contexto de los PDFs.

## Límites

- El modelo decide qué tool llamar; un prompt claro y una `description` precisa son lo que más importa.
- Las conversaciones se guardan por usuario y solo su dueño las ve.
