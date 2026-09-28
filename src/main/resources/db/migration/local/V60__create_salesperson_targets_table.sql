CREATE TABLE salesperson_targets (
    id                 UUID PRIMARY KEY,
    salesperson_id     UUID NOT NULL,
    reference_month    DATE NOT NULL,
    value_target       NUMERIC(14, 4) NOT NULL,
    order_count_target BIGINT NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    modified_at        TIMESTAMP NOT NULL,
    UNIQUE (salesperson_id, reference_month)
);
