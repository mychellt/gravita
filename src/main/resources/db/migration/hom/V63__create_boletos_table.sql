CREATE TABLE boletos (
    id               UUID PRIMARY KEY,
    receivable_id    UUID NOT NULL REFERENCES receivables (id),
    bank_integration VARCHAR(20) NOT NULL
        CHECK (bank_integration IN ('ITAU', 'BANCO_DO_BRASIL', 'BRADESCO', 'SICOOB', 'SICREDI')),
    barcode_line     VARCHAR(47) NOT NULL,
    status           VARCHAR(20) NOT NULL CHECK (status IN ('ISSUED', 'PAID', 'CANCELLED')),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL,
    modified_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_boletos_receivable_id ON boletos (receivable_id);
