CREATE TABLE voided_number_ranges (
    id             UUID PRIMARY KEY,
    company_id     UUID NOT NULL,
    document_type  VARCHAR(10) NOT NULL,
    series         VARCHAR(10) NOT NULL,
    start_number   BIGINT NOT NULL,
    end_number     BIGINT NOT NULL,
    justification  VARCHAR(500) NOT NULL,
    sefaz_protocol VARCHAR(255) NOT NULL,
    voided_at      TIMESTAMP NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_voided_number_ranges_company_id ON voided_number_ranges (company_id);
