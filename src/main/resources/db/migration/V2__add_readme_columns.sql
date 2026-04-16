ALTER TABLE repositories
    ADD COLUMN ingested_at      TIMESTAMP NULL,
    ADD COLUMN generated_readme TEXT      NULL;
