CREATE TABLE pix_charges (
    id                 UUID PRIMARY KEY,
    receivable_id      UUID NOT NULL REFERENCES receivables (id),
    dynamic_qr_payload VARCHAR(1024) NOT NULL,
    amount             NUMERIC(14, 2) NOT NULL,
    due_date           DATE NOT NULL,
    expires_at         TIMESTAMP NOT NULL,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'EXPIRED')),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    modified_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_pix_charges_receivable_id ON pix_charges (receivable_id);
CREATE INDEX idx_pix_charges_status_expires_at ON pix_charges (status, expires_at);
