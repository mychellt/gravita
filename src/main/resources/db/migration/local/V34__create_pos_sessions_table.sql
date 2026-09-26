CREATE TABLE pos_sessions (
    id                    UUID PRIMARY KEY,
    register_id           UUID NOT NULL,
    operator_id           UUID NOT NULL,
    opening_change_amount NUMERIC(14, 2) NOT NULL,
    status                VARCHAR(20) NOT NULL,
    opened_at             TIMESTAMP NOT NULL,
    closed_at             TIMESTAMP,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL,
    modified_at           TIMESTAMP NOT NULL
);

CREATE INDEX idx_pos_sessions_register_id ON pos_sessions (register_id);

-- Enforces the one-open-session-per-register invariant (UC-M3-01, AC1) at the
-- DB level, closing the race window between the app-level check and the insert.
CREATE UNIQUE INDEX uq_pos_sessions_register_open ON pos_sessions (register_id) WHERE status = 'OPEN';
