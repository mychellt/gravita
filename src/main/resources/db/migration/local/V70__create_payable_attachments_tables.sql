-- The stored bytes of a document uploaded for a payable (boleto, NF, receipt).
CREATE TABLE document_attachments (
    id           UUID PRIMARY KEY,
    payable_id   UUID NOT NULL REFERENCES payables (id),
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    content      BYTEA NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_document_attachments_payable_id ON document_attachments (payable_id);

-- The documents linked to a payable, in the order they were attached.
CREATE TABLE payable_attachments (
    payable_id   UUID NOT NULL REFERENCES payables (id),
    position     INTEGER NOT NULL,
    storage_ref  VARCHAR(255) NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    PRIMARY KEY (payable_id, position)
);
