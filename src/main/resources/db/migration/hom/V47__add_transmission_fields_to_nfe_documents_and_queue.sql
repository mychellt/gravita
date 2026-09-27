ALTER TABLE nfe_documents
    ADD COLUMN contingency_mode BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN xml_storage_ref VARCHAR(255),
    ADD COLUMN danfe_storage_ref VARCHAR(255);

ALTER TABLE nfce_contingency_queue
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP;

UPDATE nfce_contingency_queue SET next_retry_at = queued_at WHERE next_retry_at IS NULL;

ALTER TABLE nfce_contingency_queue ALTER COLUMN next_retry_at SET NOT NULL;

CREATE INDEX idx_nfce_contingency_queue_next_retry_at ON nfce_contingency_queue (next_retry_at);
CREATE UNIQUE INDEX idx_nfce_contingency_queue_nfce_sale_id ON nfce_contingency_queue (nfce_sale_id);
