CREATE TABLE price_tables (
    id                     UUID PRIMARY KEY,
    formation              VARCHAR(30) NOT NULL CHECK (formation IN ('FIXED', 'PERCENT_OVER_COST', 'PERCENT_OVER_BASE')),
    valid_from             DATE NOT NULL,
    valid_to               DATE,
    max_discount_percent   NUMERIC(5, 2),
    max_discount_behavior  VARCHAR(10) CHECK (max_discount_behavior IN ('BLOCK', 'ALERT')),
    created_at             TIMESTAMP NOT NULL,
    modified_at            TIMESTAMP NOT NULL
);

CREATE TABLE price_table_entries (
    price_table_id UUID NOT NULL REFERENCES price_tables (id),
    ref_type       VARCHAR(20) NOT NULL CHECK (ref_type IN ('PRODUCT', 'PRODUCT_CLASS')),
    reference_id   VARCHAR(255) NOT NULL,
    value          NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_price_table_entries_price_table_id ON price_table_entries (price_table_id);
