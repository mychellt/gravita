CREATE TABLE salesperson_targets (
    id                 UUID PRIMARY KEY,
    salesperson_id     UUID NOT NULL,
    target_month       VARCHAR(7) NOT NULL,
    value_target       NUMERIC(14, 4) NOT NULL,
    order_count_target INTEGER NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    modified_at        TIMESTAMP NOT NULL,
    UNIQUE (salesperson_id, target_month)
);

CREATE INDEX idx_salesperson_targets_salesperson_id ON salesperson_targets (salesperson_id);
