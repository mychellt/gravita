CREATE TABLE voided_number_ranges (
    id            UUID PRIMARY KEY,
    company_id    UUID NOT NULL,
    series        VARCHAR(20) NOT NULL,
    start_number  BIGINT NOT NULL,
    end_number    BIGINT NOT NULL,
    justification VARCHAR(500) NOT NULL,
    protocol      VARCHAR(100) NOT NULL,
    voided_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_voided_number_ranges_company_series ON voided_number_ranges (company_id, series);
