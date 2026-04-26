# Talk To Repo

An AI-powered Spring Boot application that clones source code repositories (GitHub, GitLab, etc.), indexes them into a vector store, and lets you explore and understand codebases through natural language.

## Features

- **Clone repositories** from any Git URL (GitHub, GitLab, etc.)
- **Ingest source code** into a PostgreSQL pgvector store for semantic search
- **Generate READMEs** using RAG (Retrieval-Augmented Generation) with OpenAI
- **Refresh repositories** with the latest changes via git pull
- **Re-ingest** repositories after updates

## Tech Stack

- **Java 25** / **Spring Boot 4.0.5**
- **Spring AI 2.0.0-M4**:
  - Ollama chat (`qwen3-coder`) and embedding (`nomic-embed-text-v2-moe:latest`) models, 
  - OpenAI chat (`gpt-5`) and embedding (`text-embedding-3-small`) models, 
  - Pgvector store, JDBC chat memory
- **PostgreSQL + pgvector** — persistence and vector similarity search (HNSW / cosine distance)
- **JGit 7.6** — programmatic git clone and pull
- **Flyway** — database migrations
- **Thymeleaf + thymeleaf-layout-dialect** — server-side HTML templating
- **Tailwind CSS (CDN)** + **Font Awesome (CDN)** — UI styling

## Prerequisites

- Java 25+
- Docker (for PostgreSQL + pgvector via Docker Compose)
- Ollama or OpenAI API key

## Using Ollama (default)

Install [Ollama](https://ollama.com) and pull the `qwen3-coder:latest`, `nomic-embed-text-v2-moe:latest` models

```shell
ollama pull qwen3-coder:latest
ollama pull nomic-embed-text-v2-moe:latest
```

> [!NOTE]  
> The qwen3-coder:latest model is around 18GB. 
> If you want to use a smaller model like `qwen3:latest` or `gemma4:latest`, pull those models and update the property `spring.ai.ollama.chat.model` in application.properties accordingly.

## Using OpenAI (Enable openai profile)

1. **Set your OpenAI API key:**

   ```bash
   export OPENAI_API_KEY=sk-...
   ```

2. **Run the application** (Docker Compose starts PostgreSQL automatically):

   ```bash
   ./mvnw spring-boot:run
   ```

3. Open `http://localhost:8080` in your browser.

> [!IMPORTANT]  
> The embedding models nomic-embed-text-v2-moe and text-embedding-3-small do not support the same dimension.
> So, if you want to switch from Ollama to OpenAI or vice versa, you should delete the Pgvector database and recreate.

## Usage

1. Go to **Add Repo** and enter a Git repository URL.
2. The app clones it to `~/.ttr/<repo-name>/`.
3. Click **Re-ingest** to index the source files into the vector store.
4. Click **Generate README** to produce an AI-generated overview of the repository.
