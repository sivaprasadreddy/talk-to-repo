# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Talk To Repo is an AI-powered Spring Boot application that ingests source code repositories (GitHub, GitLab, etc.), embeds them into a vector store, and answers natural language questions about the codebase.

## Tech Stack

- **Java 25** / **Spring Boot 4.0.x**
- **Spring AI 2.0.0-M4** — OpenAI model, PGVector vector store, JDBC chat memory
- **PostgreSQL + pgvector** — persistence and vector similarity search
- **Flyway** — database migrations (`src/main/resources/db/migration/`)
- **Thymeleaf + thymeleaf-layout-dialect** — server-side templating
- **Tailwind CSS (CDN)** + **Font Awesome (CDN)** — UI styling

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
