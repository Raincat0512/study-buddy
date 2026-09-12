# Study Buddy — Personal Study Assistant Agent

[中文](README.md) | [English](README_EN.md)

Feed it lecture slides, notes, or articles: an LLM summarizes and tags each document, then embeds it into a vector store. Afterwards, you chat with your own study materials in natural language — answers come with source citations, and the agent can autonomously call tools to browse your library.

> Tech stack: Java 17 · Spring Boot 3.5 · Spring AI 1.0 · PostgreSQL + pgvector · DeepSeek (chat) · Zhipu embedding-3 (embeddings) · Vanilla HTML/JS

## Architecture

```
 Import (.txt/.md/.pdf / web URL)
        │
        ▼
 ┌─────────────┐   Text extraction (jsoup / PDFBox)
 │ DocumentService
 │   ├─ SummarizationService ── LLM structured output: summary + tags (DeepSeek)
 │   ├─ TextChunker ─────────── fixed-size sliding window with overlap
 │   └─ VectorStore.add ─────── embed with embedding-3 → pgvector
 └─────────────┘
        │
        ▼ Chat (POST /api/chat)
 ┌─────────────┐
 │ ChatService  │  ① RetrievalService: question → top-k similarity search
 │              │  ② Build a system prompt with numbered [1][2] sources
 │              │  ③ Mount NoteTools (list docs / search by tag / semantic search)
 │              │  ④ DeepSeek generates the answer (with citations)
 │              │  ⑤ Session history persisted in PG for multi-turn context
 └─────────────┘
```

## Project Layout

```
src/main/java/com/raincat/studybuddy/
├── chat/        # ChatService (RAG orchestration + function calling), NoteTools, history
├── document/    # Ingestion/extraction/retrieval: PDFBox, jsoup, TextChunker, RetrievalService
├── chunk/       # Character-based chunking (fixed size + overlap)
├── summarize/   # LLM structured summary + tags
├── config/      # Configuration properties, global exception handling
└── StudyBuddyApplication.java
src/main/resources/static/   # Vanilla HTML/JS frontend (library page + chat page)
scripts/dim_check.py         # Diagnostic script: check the embedding model's output dimensions
```

## Quick Start

### 1. Get API Keys

- DeepSeek (chat): https://platform.deepseek.com
- Zhipu BigModel (embedding-3): https://open.bigmodel.cn

```bash
cp .env.example .env   # fill in both keys
```

### 2. Run

**Option A: fully containerized (requires Docker)**

```bash
docker compose up --build
```

**Option B: database in Docker, app on host (recommended for development)**

```bash
docker compose up -d db
mvn spring-boot:run
```

Open http://localhost:8080 — import documents on the "Library" page, ask questions on the "Chat" page.

### 3. Key Configuration (application.yml)

| Property | Default | Description |
|---|---|---|
| `app.chunk-size` / `app.chunk-overlap` | 500 / 80 | Chunk length and overlap window (characters) |
| `app.rag.top-k` | 4 | Number of chunks retrieved per query |
| `app.rag.similarity-threshold` | 0.30 | Chunks below this similarity are excluded from the prompt |
| `app.rag.history-messages` | 10 | History messages carried for multi-turn chat |
| `spring.ai.vectorstore.pgvector.dimensions` | 2048 | Must match the embedding model's output dimensions |

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents/file` | multipart upload of .txt/.md/.pdf; synchronous summarize + chunk + embed |
| POST | `/api/documents/url` | `{"url": "..."}` fetch a web page and ingest it |
| GET | `/api/documents` | list documents |
| GET/DELETE | `/api/documents/{id}` | detail / delete (including vector cleanup) |
| POST | `/api/chat` | `{"sessionId","message"}` → `{"answer"}` |
| GET/DELETE | `/api/chat/history?sessionId=` / `/api/chat?sessionId=` | history / clear a session |

## Known Limitations (explicitly out of scope for v1)

- PDF text-only extraction: images, tables, PPT, and scanned documents are not supported (no OCR)
- No algorithmic main-content extraction for web pages — the full `<body>` is taken
- No login, single user; no streaming output (answers return as one block)
- Tag search is a LIKE-contains match, not a managed tag system

## Future Work

- SSE streaming output; async ingestion with progress
- Hybrid retrieval (BM25 + vectors) and reranking
- Proper tag system + tag-based browsing; summary regeneration
- Spaced-repetition review reminders
