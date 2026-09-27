CREATE TABLE follow_up_rules (
    id                   UUID PRIMARY KEY,
    days_without_contact INTEGER NOT NULL,
    target               VARCHAR(20) NOT NULL CHECK (target IN ('CUSTOMER', 'OPPORTUNITY')),
    notify_owner         BOOLEAN NOT NULL DEFAULT FALSE,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    modified_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_follow_up_rules_active ON follow_up_rules (active);
