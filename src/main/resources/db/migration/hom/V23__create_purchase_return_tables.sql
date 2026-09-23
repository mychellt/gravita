CREATE TABLE purchase_returns (
    id             UUID PRIMARY KEY,
    receipt_id     UUID NOT NULL REFERENCES purchase_receipts (id),
    total          BOOLEAN NOT NULL,
    return_nfe_ref VARCHAR(255),
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE TABLE purchase_return_items (
    purchase_return_id UUID NOT NULL REFERENCES purchase_returns (id),
    product_id         UUID NOT NULL,
    quantity           NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_purchase_return_items_purchase_return_id ON purchase_return_items (purchase_return_id);
CREATE INDEX idx_purchase_returns_receipt_id ON purchase_returns (receipt_id);
