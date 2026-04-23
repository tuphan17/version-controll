-- rough sketch of what jpa makes; don't rely on this file for migrations

CREATE TABLE repo_registry (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    filesystem_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_repo_registry_name ON repo_registry (name);
