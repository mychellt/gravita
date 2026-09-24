CREATE TABLE physical_counts (
    id               UUID PRIMARY KEY,
    scope            VARCHAR(20) NOT NULL,
    product_group_id VARCHAR(255),
    warehouse_id     UUID NOT NULL,
    status           VARCHAR(20) NOT NULL,
    started_by       UUID NOT NULL,
    started_at       TIMESTAMP NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL,
    modified_at      TIMESTAMP NOT NULL
);

CREATE TABLE physical_count_lines (
    physical_count_id UUID NOT NULL REFERENCES physical_counts (id),
    product_id        UUID NOT NULL,
    system_quantity   NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_physical_count_lines_physical_count_id ON physical_count_lines (physical_count_id);
CREATE INDEX idx_physical_counts_warehouse_id ON physical_counts (warehouse_id);
