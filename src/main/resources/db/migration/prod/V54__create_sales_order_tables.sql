CREATE TABLE sales_orders (
    id              UUID PRIMARY KEY,
    origin_quote_id UUID NOT NULL REFERENCES quotes (id),
    customer_id     UUID NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'IN_SEPARATION', 'INVOICED', 'CANCELLED')),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE TABLE sales_order_items (
    sales_order_id        UUID NOT NULL REFERENCES sales_orders (id),
    line_index             INTEGER NOT NULL,
    product_or_service_id  UUID NOT NULL,
    quantity                NUMERIC(14, 4) NOT NULL,
    unit_price              NUMERIC(14, 4) NOT NULL,
    discount                NUMERIC(14, 4) NOT NULL,
    PRIMARY KEY (sales_order_id, line_index)
);

CREATE INDEX idx_sales_orders_customer_id ON sales_orders (customer_id);
CREATE INDEX idx_sales_orders_origin_quote_id ON sales_orders (origin_quote_id);
