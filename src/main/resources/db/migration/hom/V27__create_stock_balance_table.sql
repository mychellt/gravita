CREATE TABLE stock_balances (
    id           UUID PRIMARY KEY,
    product_id   UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    on_hand      NUMERIC(14, 4) NOT NULL,
    reserved     NUMERIC(14, 4) NOT NULL,
    in_transit   NUMERIC(14, 4) NOT NULL,
    average_cost NUMERIC(14, 4) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_stock_balances_product_warehouse UNIQUE (product_id, warehouse_id)
);

CREATE INDEX idx_stock_balances_product_id ON stock_balances (product_id);
