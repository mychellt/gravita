CREATE TABLE stock_movements (
    id               UUID PRIMARY KEY,
    type             VARCHAR(20) NOT NULL,
    product_id       UUID NOT NULL,
    warehouse_id     UUID NOT NULL,
    quantity         NUMERIC(14, 4) NOT NULL,
    unit_cost        NUMERIC(14, 4) NOT NULL,
    lot_code         VARCHAR(60),
    origin_reference VARCHAR(255) NOT NULL,
    user_id          UUID NOT NULL,
    movement_at      TIMESTAMP NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL,
    modified_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_stock_movements_product_warehouse ON stock_movements (product_id, warehouse_id);

CREATE TABLE stock_movement_serial_numbers (
    stock_movement_id UUID NOT NULL REFERENCES stock_movements (id),
    serial_number      VARCHAR(100) NOT NULL
);

CREATE TABLE lots (
    id           UUID PRIMARY KEY,
    product_id   UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    code         VARCHAR(60) NOT NULL,
    expiry_date  DATE NOT NULL,
    quantity     NUMERIC(14, 4) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_lots_product_warehouse_code UNIQUE (product_id, warehouse_id, code)
);

CREATE TABLE serial_units (
    id            UUID PRIMARY KEY,
    product_id    UUID NOT NULL,
    warehouse_id  UUID NOT NULL,
    serial_number VARCHAR(100) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL,
    CONSTRAINT uq_serial_units_product_serial UNIQUE (product_id, serial_number)
);
