CREATE TABLE stock_reservations (
    id           UUID PRIMARY KEY,
    order_ref    UUID NOT NULL,
    product_id   UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity     NUMERIC(14, 4) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_stock_reservations_order_ref ON stock_reservations (order_ref);
CREATE INDEX idx_stock_reservations_product_warehouse ON stock_reservations (product_id, warehouse_id);
