CREATE TABLE digital_certificates (
    id                     UUID PRIMARY KEY,
    company_id             UUID NOT NULL REFERENCES companies (id),
    type                   VARCHAR(10) NOT NULL CHECK (type IN ('A1', 'A3')),
    encrypted_pfx_payload  BYTEA NOT NULL,
    encrypted_password     TEXT NOT NULL,
    expires_at             TIMESTAMP NOT NULL,
    uploaded_at            TIMESTAMP NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    modified_at            TIMESTAMP NOT NULL,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_digital_certificates_company_id UNIQUE (company_id)
);
