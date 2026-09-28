CREATE TABLE payables (
    id          UUID PRIMARY KEY,
    supplier_id UUID,
    origin      VARCHAR(20) NOT NULL CHECK (origin IN ('PURCHASE_RECEIPT', 'MANUAL')),
    amount      NUMERIC(14, 2) NOT NULL,
    due_date    DATE NOT NULL,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'APPROVED', 'PAID', 'CANCELLED')),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payables_supplier_id ON payables (supplier_id);
CREATE INDEX idx_payables_status ON payables (status);

-- How a payable's amount is split across cost centers, in the order given (empty when not split).
CREATE TABLE payable_cost_center_splits (
    payable_id     UUID NOT NULL REFERENCES payables (id),
    position       INTEGER NOT NULL,
    cost_center_id UUID NOT NULL,
    percent        NUMERIC(5, 2) NOT NULL,
    PRIMARY KEY (payable_id, position)
);
