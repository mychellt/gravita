CREATE TABLE cash_movements (
    id            UUID PRIMARY KEY,
    session_id    UUID NOT NULL,
    type          VARCHAR(20) NOT NULL,
    amount        NUMERIC(14, 2) NOT NULL,
    justification VARCHAR(500) NOT NULL,
    occurred_at   TIMESTAMP NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_cash_movements_session_id ON cash_movements (session_id);
