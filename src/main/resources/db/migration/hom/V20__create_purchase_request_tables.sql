CREATE TABLE purchase_requests (
    id             UUID PRIMARY KEY,
    origin         VARCHAR(30) NOT NULL CHECK (origin IN ('USER', 'MIN_STOCK_TRIGGER', 'SALES_ORDER_DEMAND')),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'QUOTED', 'CONVERTED', 'CANCELLED')),
    requested_by   UUID,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE TABLE purchase_request_items (
    purchase_request_id UUID NOT NULL REFERENCES purchase_requests (id),
    product_id           UUID NOT NULL,
    quantity             NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_purchase_request_items_purchase_request_id ON purchase_request_items (purchase_request_id);
