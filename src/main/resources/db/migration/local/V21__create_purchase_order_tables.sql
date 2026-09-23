CREATE TABLE purchase_orders (
    id                UUID PRIMARY KEY,
    request_id        UUID NOT NULL REFERENCES purchase_requests (id),
    quotation_id      UUID,
    supplier_id       UUID NOT NULL,
    approval_required BOOLEAN NOT NULL,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PARTIALLY_RECEIVED', 'CLOSED', 'CANCELLED')),
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL
);

CREATE TABLE purchase_order_items (
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders (id),
    product_id        UUID NOT NULL,
    quantity          NUMERIC(14, 4) NOT NULL,
    unit_price        NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_purchase_order_items_purchase_order_id ON purchase_order_items (purchase_order_id);
CREATE INDEX idx_purchase_orders_request_id ON purchase_orders (request_id);
