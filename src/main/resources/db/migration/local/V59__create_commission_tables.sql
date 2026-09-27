CREATE TABLE commission_rates (
    id             UUID PRIMARY KEY,
    salesperson_id UUID NOT NULL,
    product_id     UUID NOT NULL,
    rate           NUMERIC(7, 4) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL,
    UNIQUE (salesperson_id, product_id)
);

CREATE TABLE commissions (
    id             UUID PRIMARY KEY,
    salesperson_id UUID NOT NULL,
    product_id     UUID NOT NULL,
    sales_order_id UUID NOT NULL REFERENCES sales_orders (id),
    rate           NUMERIC(7, 4) NOT NULL,
    amount         NUMERIC(14, 4) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_commissions_salesperson_id ON commissions (salesperson_id);
CREATE INDEX idx_commissions_sales_order_id ON commissions (sales_order_id);
