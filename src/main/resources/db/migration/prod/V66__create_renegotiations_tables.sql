ALTER TABLE receivables DROP CONSTRAINT receivables_origin_check;
ALTER TABLE receivables
    ADD CONSTRAINT receivables_origin_check CHECK (origin IN ('INVOICING', 'MANUAL', 'RENEGOTIATION'));

CREATE TABLE renegotiations (
    id              UUID PRIMARY KEY,
    customer_id     UUID NOT NULL,
    renegotiated_at TIMESTAMP NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_renegotiations_customer_id ON renegotiations (customer_id);

-- The titles a renegotiation replaced, in the order they were given. A title is renegotiated at most once.
CREATE TABLE renegotiation_original_receivables (
    renegotiation_id UUID NOT NULL REFERENCES renegotiations (id),
    position         INTEGER NOT NULL,
    receivable_id    UUID NOT NULL REFERENCES receivables (id),
    PRIMARY KEY (renegotiation_id, position),
    CONSTRAINT uq_renegotiation_original_receivable UNIQUE (receivable_id)
);

-- The titles created from the agreed plan, in installment order.
CREATE TABLE renegotiation_new_receivables (
    renegotiation_id UUID NOT NULL REFERENCES renegotiations (id),
    position         INTEGER NOT NULL,
    receivable_id    UUID NOT NULL REFERENCES receivables (id),
    PRIMARY KEY (renegotiation_id, position),
    CONSTRAINT uq_renegotiation_new_receivable UNIQUE (receivable_id)
);
