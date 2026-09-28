CREATE TABLE settlements (
    id            UUID PRIMARY KEY,
    receivable_id UUID NOT NULL REFERENCES receivables (id),
    amount        NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    interest      NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (interest >= 0),
    fine          NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (fine >= 0),
    discount      NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    surcharge     NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (surcharge >= 0),
    method        VARCHAR(20) NOT NULL CHECK (method IN ('AUTOMATIC_CNAB', 'MANUAL', 'PIX')),
    settled_at    TIMESTAMP NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_settlements_receivable_id ON settlements (receivable_id);
