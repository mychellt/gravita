CREATE TABLE user_activation_tokens
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users (id),
    expires_at  TIMESTAMP    NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    modified_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_user_activation_token_user_id ON user_activation_tokens (user_id);
