-- The back office's cash box (§9.3), separate from the PDV's pos_sessions/cash_movements (M3).
CREATE TABLE internal_cash_boxes (
    id          UUID PRIMARY KEY,
    balance     NUMERIC(14, 2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

-- There is a single physical box; RecordInternalCashMovementUseCase looks it up by this fixed id.
INSERT INTO internal_cash_boxes (id, balance, created_at, modified_at)
VALUES ('00000000-0000-0000-0000-000000000001', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Transfers between the cash box and the bank; FROM_BANK adds to the box balance, TO_BANK subtracts.
CREATE TABLE internal_cash_movements (
    id            UUID PRIMARY KEY,
    cash_box_id   UUID NOT NULL REFERENCES internal_cash_boxes (id),
    direction     VARCHAR(20) NOT NULL CHECK (direction IN ('TO_BANK', 'FROM_BANK')),
    amount        NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    justification VARCHAR(500) NOT NULL,
    occurred_at   TIMESTAMP NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_internal_cash_movements_cash_box_id ON internal_cash_movements (cash_box_id, occurred_at);
