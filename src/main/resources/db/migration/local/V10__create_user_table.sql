CREATE TABLE users (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    profile_id        UUID NOT NULL REFERENCES profiles (id),
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_profile_id ON users (profile_id);
