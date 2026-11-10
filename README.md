# Study Buddy — 个人学习助手 Agent

[中文](README.md) | [English](README_EN.md)

把课件、笔记、文章丢进去，LLM 自动摘要打标签、向量化入库；之后用自然语言对话检索自己的学习资料，回答带来源引用，agent 还能自主调用工具浏览资料库。

> 技术栈：Java 17 · Spring Boot 3.5 · Spring AI 1.0 · PostgreSQL + pgvector · DeepSeek（对话）· 智谱 embedding-3（向量）· 原生 HTML/JS

## 架构

```
 导入（.txt/.md/.pdf/网页 URL）
        │
        ▼
 ┌─────────────┐   正文抽取（jsoup / PDFBox）
 │ DocumentService
 │   ├─ SummarizationService ── LLM 结构化输出：摘要 + 标签（DeepSeek）
 │   ├─ TextChunker ─────────── 固定长度 + 重叠滑动窗口分块
 │   └─ VectorStore.add ─────── embedding-3 向量化 → pgvector
 └─────────────┘
        │
        ▼ 对话（POST /api/chat）
 ┌─────────────┐
 │ ChatService  │  ① RetrievalService：问题 → 相似度检索 top-k 片段
 │              │  ② 组装带 [1][2] 编号的参考资料 system prompt
 │              │  ③ 挂载 NoteTools 工具（列资料/按标签查/语义检索）
 │              │  ④ DeepSeek 生成回答（带来源引用）
 │              │  ⑤ 会话历史存 PG，多轮上下文
 └─────────────┘
```

## 项目结构

```
src/main/java/com/raincat/studybuddy/
├── chat/        # ChatService（RAG 编排 + function calling）、NoteTools 工具、会话历史
├── document/    # 导入/抽取/检索：PDFBox、jsoup、TextChunker、RetrievalService
├── chunk/       # 字符分块（固定长度 + 重叠窗口）
├── summarize/   # LLM 结构化摘要 + 标签
├── config/      # 配置属性、全局异常处理
└── StudyBuddyApplication.java
src/main/resources/static/   # 原生 HTML/JS 前端（资料库 + 对话两个页面）
scripts/dim_check.py         # 查询 embedding 模型实际返回维度的诊断脚本
```

## 快速开始

### 1. 准备 API Key

- DeepSeek（对话）：https://platform.deepseek.com
- 智谱（向量 embedding-3）：https://open.bigmodel.cn

```bash
cp .env.example .env   # 填入两个 Key
```

### 2. 启动

**方式 A：完整容器化（需 Docker）**

```bash
docker compose up --build
```

**方式 B：只起数据库，本地跑应用（推荐开发期用）**

```bash
docker compose up -d db
mvn spring-boot:run
```

打开 http://localhost:8080 —— 「资料库」页导入，「对话」页提问。

### 3. 主要配置（application.yml）

| 配置 | 默认值 | 说明 |
|---|---|---|
| `app.chunk-size` / `app.chunk-overlap` | 500 / 80 | 分块长度与重叠窗口（字符） |
| `app.rag.top-k` | 4 | 每次检索召回的片段数 |
| `app.rag.similarity-threshold` | 0.30 | 相似度阈值，低于它的片段不进 prompt |
| `app.rag.history-messages` | 10 | 多轮对话携带的历史条数 |
| `spring.ai.vectorstore.pgvector.dimensions` | 2048 | 需与 embedding 模型输出维度一致 |

## API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/documents/file` | multipart 上传 .txt/.md/.pdf，同步完成摘要+分块+向量化 |
| POST | `/api/documents/url` | `{"url": "..."}` 抓取网页导入 |
| GET | `/api/documents` | 资料列表 |
| GET/DELETE | `/api/documents/{id}` | 详情 / 删除（含向量清理） |
| POST | `/api/chat` | `{"sessionId","message"}` → `{"answer"}` |
| GET/DELETE | `/api/chat/history?sessionId=` / `/api/chat?sessionId=` | 历史 / 清空会话 |
