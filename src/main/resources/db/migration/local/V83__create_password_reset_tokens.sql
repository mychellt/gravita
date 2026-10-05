-- Single-use password reset links, kept apart from activation_tokens so a reset link can never activate an account.
-- Only the SHA-256 hash of the secret is stored; modified_at is set once, when the token is consumed.
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    modified_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
