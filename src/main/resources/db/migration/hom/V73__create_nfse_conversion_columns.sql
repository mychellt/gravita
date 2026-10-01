-- M4-03: RPS -> NFSe conversion assigns the NFSe series/number (per company + municipality) and moves it to DRAFT.
ALTER TABLE nfse_documents ADD COLUMN nfse_series VARCHAR(10);
ALTER TABLE nfse_documents ADD COLUMN nfse_number  BIGINT;
ALTER TABLE nfse_documents ADD COLUMN draft_at     TIMESTAMP;

ALTER TABLE nfse_documents ADD CONSTRAINT uk_nfse_documents_nfse_number
    UNIQUE (provider_company_id, provider_municipality_ibge, nfse_series, nfse_number);

-- Next NFSe number of each (company, municipality); independent of document_series (NFe/NFCe/RPS).
CREATE TABLE nfse_number_sequences (
    id                UUID PRIMARY KEY,
    company_id        UUID NOT NULL REFERENCES companies (id),
    municipality_ibge VARCHAR(7) NOT NULL,
    series            VARCHAR(10) NOT NULL,
    next_number       BIGINT NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL,
    CONSTRAINT uk_nfse_number_sequences_scope UNIQUE (company_id, municipality_ibge)
);
