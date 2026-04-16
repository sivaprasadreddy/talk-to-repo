CREATE TABLE repositories (
    id         BIGSERIAL PRIMARY KEY,
    repo_url   VARCHAR(1000) NOT NULL UNIQUE,
    repo_name  VARCHAR(500)  NOT NULL,
    local_path VARCHAR(2000) NOT NULL,
    cloned_at  TIMESTAMP     NOT NULL
);
