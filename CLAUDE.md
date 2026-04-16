# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Talk To Repo is an AI-powered Spring Boot application that clones source code repositories (GitHub, GitLab, etc.), indexes them into a PostgreSQL pgvector store, and answers natural language questions about the codebase via RAG (Retrieval-Augmented Generation).

## Tech Stack

- **Java 25** / **Spring Boot 4.0.5**
- **Spring AI 2.0.0-M4** — OpenAI chat (`gpt-5`) and embedding (`text-embedding-3-small`) models, pgvector store, JDBC chat memory
- **PostgreSQL + pgvector** — persistence and vector similarity search (HNSW / cosine distance, 1536 dimensions)
- **JGit 7.6.0** — programmatic git clone and pull
- **Flyway** — database migrations (`src/main/resources/db/migration/`)
- **Thymeleaf + thymeleaf-layout-dialect** — server-side HTML templating
- **Tailwind CSS (CDN)** + **Font Awesome 7.0.1 (CDN)** — UI styling

## Commands

```bash
# Build (skipping tests)
./mvnw clean package -DskipTests

# Run all tests (starts Testcontainers automatically)
./mvnw test

# Run a single test class
./mvnw test -Dtest=MyTestClass

# Run the app (requires Docker — Spring Boot auto-starts compose.yaml)
./mvnw spring-boot:run

# Run with Testcontainers instead of Docker Compose (dev mode)
./mvnw spring-boot:test-run -Dspring-boot.run.main-class=dev.sivalabs.ttr.TestTalkToRepoApplication
```
