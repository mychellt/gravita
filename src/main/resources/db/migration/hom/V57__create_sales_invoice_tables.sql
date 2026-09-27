CREATE TABLE sales_invoices (
    id              UUID PRIMARY KEY,
    sales_order_id  UUID NOT NULL REFERENCES sales_orders (id),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('ISSUED')),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE TABLE sales_invoice_fiscal_documents (
    sales_invoice_id UUID NOT NULL REFERENCES sales_invoices (id),
    line_index       INTEGER NOT NULL,
    document_type    VARCHAR(10) NOT NULL CHECK (document_type IN ('NFE', 'NFCE', 'NFSE')),
    document_id      UUID NOT NULL,
    PRIMARY KEY (sales_invoice_id, line_index)
);

CREATE INDEX idx_sales_invoices_sales_order_id ON sales_invoices (sales_order_id);
