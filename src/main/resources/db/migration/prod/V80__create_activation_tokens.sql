-- Self-service signups start as PENDING_ACTIVATION (cannot log in) until they confirm the e-mailed link.
ALTER TABLE users DROP CONSTRAINT users_status_check;
ALTER TABLE users
    ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_ACTIVATION'));

-- Single-use activation links. Only the SHA-256 hash of the secret is stored.
CREATE TABLE activation_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL
);

CREATE INDEX idx_activation_tokens_user_id ON activation_tokens (user_id);
