CREATE TABLE receivables (
    id           UUID PRIMARY KEY,
    customer_id  UUID NOT NULL,
    origin       VARCHAR(20) NOT NULL CHECK (origin IN ('INVOICING', 'MANUAL')),
    amount       NUMERIC(14, 2) NOT NULL,
    due_date     DATE NOT NULL,
    installments INTEGER,
    status       VARCHAR(20) NOT NULL
        CHECK (status IN ('OPEN', 'PARTIALLY_SETTLED', 'SETTLED', 'RENEGOTIATED', 'CANCELLED')),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_receivables_customer_id ON receivables (customer_id);
CREATE INDEX idx_receivables_status ON receivables (status);
