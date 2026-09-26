CREATE TABLE nfce_contingency_queue (
    id           UUID PRIMARY KEY,
    nfce_sale_id UUID NOT NULL REFERENCES nfce_sales (id),
    queued_at    TIMESTAMP NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_nfce_contingency_queue_sale_id ON nfce_contingency_queue (nfce_sale_id);
