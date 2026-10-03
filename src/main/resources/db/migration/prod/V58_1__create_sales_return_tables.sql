CREATE TABLE sales_returns (
    id                       UUID PRIMARY KEY,
    sales_order_id           UUID NOT NULL REFERENCES sales_orders (id),
    total                    BOOLEAN NOT NULL DEFAULT FALSE,
    return_nfe_document_type VARCHAR(10) CHECK (return_nfe_document_type IN ('NFE', 'NFCE', 'NFSE')),
    return_nfe_document_id   UUID,
    active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL,
    modified_at              TIMESTAMP NOT NULL
);

CREATE TABLE sales_return_items (
    sales_return_id       UUID NOT NULL REFERENCES sales_returns (id),
    line_index            INTEGER NOT NULL,
    product_or_service_id UUID NOT NULL,
    quantity               NUMERIC(14, 4) NOT NULL,
    PRIMARY KEY (sales_return_id, line_index)
);

CREATE INDEX idx_sales_returns_sales_order_id ON sales_returns (sales_order_id);
