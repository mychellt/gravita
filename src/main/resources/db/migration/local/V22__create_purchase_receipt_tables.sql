CREATE TABLE purchase_receipts (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES purchase_orders (id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_CONFERENCE', 'CONFERENCE_COMPLETED', 'CONFIRMED')),
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE TABLE purchase_receipt_items (
    purchase_receipt_id UUID NOT NULL REFERENCES purchase_receipts (id),
    product_id          UUID NOT NULL,
    ordered_qty         NUMERIC(14, 4) NOT NULL,
    received_qty        NUMERIC(14, 4) NOT NULL
);

CREATE TABLE purchase_receipt_installments (
    purchase_receipt_id UUID NOT NULL REFERENCES purchase_receipts (id),
    amount              NUMERIC(14, 4) NOT NULL,
    due_date            DATE NOT NULL
);

CREATE INDEX idx_purchase_receipt_items_purchase_receipt_id ON purchase_receipt_items (purchase_receipt_id);
CREATE INDEX idx_purchase_receipt_installments_purchase_receipt_id ON purchase_receipt_installments (purchase_receipt_id);
CREATE INDEX idx_purchase_receipts_order_id ON purchase_receipts (order_id);
