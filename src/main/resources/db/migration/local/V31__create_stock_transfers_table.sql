CREATE TABLE stock_transfers (
    id                       UUID PRIMARY KEY,
    product_id               UUID NOT NULL,
    source_warehouse_id      UUID NOT NULL,
    destination_warehouse_id UUID NOT NULL,
    quantity                 NUMERIC(14, 4) NOT NULL,
    status                   VARCHAR(20) NOT NULL,
    active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL,
    modified_at              TIMESTAMP NOT NULL
);

CREATE INDEX idx_stock_transfers_product_source ON stock_transfers (product_id, source_warehouse_id);
CREATE INDEX idx_stock_transfers_product_destination ON stock_transfers (product_id, destination_warehouse_id);
