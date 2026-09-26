CREATE TABLE tax_rate_rules (
    id                         UUID PRIMARY KEY,
    ncm                        VARCHAR(8) NOT NULL,
    origin_state               VARCHAR(2) NOT NULL,
    destination_state          VARCHAR(2) NOT NULL,
    regime                     VARCHAR(20) NOT NULL,
    operation_type             VARCHAR(30) NOT NULL,
    tax_type                   VARCHAR(10) NOT NULL,
    rate_percentage            NUMERIC(7, 4) NOT NULL,
    base_reduction_percentage  NUMERIC(7, 4) NOT NULL DEFAULT 0,
    mva_percentage             NUMERIC(7, 4) NOT NULL DEFAULT 0,
    active                     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMP NOT NULL,
    modified_at                TIMESTAMP NOT NULL
);

CREATE INDEX idx_tax_rate_rules_lookup ON tax_rate_rules (ncm, origin_state, destination_state, regime, operation_type);
