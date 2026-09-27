ALTER TABLE nfe_documents
    ADD COLUMN authorized_at TIMESTAMP,
    ADD COLUMN cancellation_justification TEXT,
    ADD COLUMN cancelled_at TIMESTAMP;
