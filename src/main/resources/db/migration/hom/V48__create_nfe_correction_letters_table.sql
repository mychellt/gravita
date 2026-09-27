CREATE TABLE nfe_correction_letters (
    nfe_document_id UUID NOT NULL REFERENCES nfe_documents (id),
    sequence_number INTEGER NOT NULL,
    text            TEXT NOT NULL,
    protocol        VARCHAR(255) NOT NULL,
    issued_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_nfe_correction_letters_nfe_document_id ON nfe_correction_letters (nfe_document_id);
