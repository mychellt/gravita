CREATE TABLE quotes (
    id          UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    valid_until DATE NOT NULL,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'EXPIRED', 'CONVERTED')),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE TABLE quote_items (
    quote_id              UUID NOT NULL REFERENCES quotes (id),
    line_index            INTEGER NOT NULL,
    product_or_service_id UUID NOT NULL,
    quantity              NUMERIC(14, 4) NOT NULL,
    unit_price            NUMERIC(14, 4) NOT NULL,
    discount              NUMERIC(14, 4) NOT NULL,
    PRIMARY KEY (quote_id, line_index)
);

CREATE INDEX idx_quotes_customer_id ON quotes (customer_id);
