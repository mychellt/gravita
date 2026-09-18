CREATE TABLE companies (
    id                UUID PRIMARY KEY,
    cnpj              VARCHAR(14) NOT NULL,
    ie                VARCHAR(20) NOT NULL,
    im                VARCHAR(20) NOT NULL,
    cnae              VARCHAR(10) NOT NULL,
    tax_regime        VARCHAR(20) NOT NULL CHECK (tax_regime IN ('SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'LUCRO_REAL')),
    simples_optante   BOOLEAN NOT NULL DEFAULT FALSE,
    sefaz_environment VARCHAR(20) NOT NULL CHECK (sefaz_environment IN ('PRODUCTION', 'HOMOLOGATION')),
    address           VARCHAR(255) NOT NULL,
    issuing_email     VARCHAR(255) NOT NULL,
    phone             VARCHAR(20) NOT NULL,
    logo_url          VARCHAR(255),
    parent_company_id UUID REFERENCES companies (id),
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL,
    CONSTRAINT uq_companies_cnpj UNIQUE (cnpj)
);

CREATE INDEX idx_companies_parent_company_id ON companies (parent_company_id);

CREATE TABLE document_series (
    id            UUID PRIMARY KEY,
    company_id    UUID NOT NULL REFERENCES companies (id),
    document_type VARCHAR(10) NOT NULL CHECK (document_type IN ('NFE', 'NFCE', 'NFSE')),
    series        VARCHAR(10),
    next_number   BIGINT,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL,
    CONSTRAINT uq_document_series_company_type UNIQUE (company_id, document_type)
);

CREATE INDEX idx_document_series_company_id ON document_series (company_id);
