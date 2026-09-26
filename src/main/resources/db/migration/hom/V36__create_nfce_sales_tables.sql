CREATE TABLE nfce_sales (
    id             UUID PRIMARY KEY,
    session_id     UUID NOT NULL,
    total_discount NUMERIC(14, 2) NOT NULL,
    change_given   NUMERIC(14, 2) NOT NULL,
    customer_cpf   VARCHAR(11),
    status         VARCHAR(20) NOT NULL,
    registered_at  TIMESTAMP NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_nfce_sales_session_id ON nfce_sales (session_id);

CREATE TABLE nfce_sale_items (
    nfce_sale_id   UUID NOT NULL REFERENCES nfce_sales (id),
    product_id     UUID NOT NULL,
    quantity       NUMERIC(14, 4) NOT NULL,
    unit_price     NUMERIC(14, 4) NOT NULL,
    item_discount  NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_nfce_sale_items_nfce_sale_id ON nfce_sale_items (nfce_sale_id);

CREATE TABLE nfce_sale_payments (
    nfce_sale_id UUID NOT NULL REFERENCES nfce_sales (id),
    method       VARCHAR(20) NOT NULL,
    amount       NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_nfce_sale_payments_nfce_sale_id ON nfce_sale_payments (nfce_sale_id);
