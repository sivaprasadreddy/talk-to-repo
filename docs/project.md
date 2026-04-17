# Talk To Repo

An AI-powered Spring Boot application that clones source code repositories (GitHub, GitLab, etc.), 
indexes them into a vector store, and lets you explore and understand codebases through natural language.

## Features

- **Clone repositories** from any Git URL (GitHub, GitLab, etc.)
- **Ingest source code** into a PostgreSQL pgvector store for semantic search
- **Generate READMEs** using RAG (Retrieval-Augmented Generation) with OpenAI
- **Refresh repositories** with the latest changes via git pull
- **Re-ingest** repositories after updates

## Tech Stack

- **Java 25** / **Spring Boot 4.0.5**
- **Spring AI 2.0.0-M4** — OpenAI chat (`gpt-5`) and embedding (`text-embedding-3-small`) models, pgvector store, JDBC chat memory
- **PostgreSQL + pgvector** — persistence and vector similarity search (HNSW / cosine distance)
- **JGit 7.6** — programmatic git clone and pull
- **Flyway** — database migrations
- **Thymeleaf + thymeleaf-layout-dialect** — server-side HTML templating
- **Tailwind CSS (CDN)** + **Font Awesome (CDN)** — UI styling

## Architecture
The application follows a simple package structure:

- `dev.sivalabs.ttr.domain`: Contains all the domain layer code.
- `dev.sivalabs.ttr.web`: Contains all the web layer code.