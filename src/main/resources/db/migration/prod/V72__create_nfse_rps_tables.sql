-- M4-02: RPS lives in nfse_documents (status = 'RPS') until converted to an NFSe (M4-03).
CREATE TABLE nfse_documents (
    id                              UUID PRIMARY KEY,
    status                          VARCHAR(20) NOT NULL,
    provider_company_id             UUID NOT NULL REFERENCES companies (id),
    provider_municipality_ibge      VARCHAR(7) NOT NULL,
    tomador_person_id               UUID,
    tomador_document                VARCHAR(14) NOT NULL,
    tomador_person_type             VARCHAR(20) NOT NULL,
    tomador_name                    VARCHAR(255) NOT NULL,
    tomador_municipality_ibge       VARCHAR(7),
    tomador_street                  VARCHAR(255),
    tomador_number                  VARCHAR(20),
    tomador_complement              VARCHAR(255),
    tomador_neighborhood            VARCHAR(255),
    tomador_zip_code                VARCHAR(10),
    tomador_state                   VARCHAR(2),
    service_code                    VARCHAR(5) NOT NULL,
    place_of_provision              VARCHAR(10) NOT NULL,
    iss_municipality_ibge           VARCHAR(7) NOT NULL,
    service_amount                  NUMERIC(14, 2) NOT NULL,
    iss_rate                        NUMERIC(7, 4) NOT NULL,
    iss_amount                      NUMERIC(14, 2) NOT NULL,
    iss_rate_override_justification VARCHAR(500),
    discrimination                  TEXT NOT NULL,
    rps_series                      VARCHAR(10) NOT NULL,
    rps_number                      BIGINT NOT NULL,
    document_created_at             TIMESTAMP NOT NULL,
    active                          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP NOT NULL,
    modified_at                     TIMESTAMP NOT NULL,
    CONSTRAINT uk_nfse_documents_rps_number UNIQUE (provider_company_id, rps_series, rps_number)
);

CREATE INDEX idx_nfse_documents_provider_company_id ON nfse_documents (provider_company_id);
CREATE INDEX idx_nfse_documents_status ON nfse_documents (status);

CREATE TABLE nfse_withholdings (
    nfse_document_id UUID NOT NULL REFERENCES nfse_documents (id),
    tax_type         VARCHAR(20) NOT NULL,
    base             NUMERIC(14, 2) NOT NULL,
    rate_percentage  NUMERIC(7, 4) NOT NULL,
    amount           NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_nfse_withholdings_nfse_document_id ON nfse_withholdings (nfse_document_id);

-- Parameterized service-tax table: NULL municipality_ibge / regime mean "any".
CREATE TABLE service_tax_rules (
    id                UUID PRIMARY KEY,
    service_code      VARCHAR(5) NOT NULL,
    municipality_ibge VARCHAR(7),
    regime            VARCHAR(20),
    tax_type          VARCHAR(20) NOT NULL,
    rate_percentage   NUMERIC(7, 4) NOT NULL,
    withholding       VARCHAR(20) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_service_tax_rules_lookup ON service_tax_rules (service_code, municipality_ibge);

-- A municipality's own service list; a municipality with no rows here falls back to the LC 116/2003 list alone.
CREATE TABLE municipal_service_codes (
    id                UUID PRIMARY KEY,
    municipality_ibge VARCHAR(7) NOT NULL,
    service_code      VARCHAR(5) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL,
    CONSTRAINT uk_municipal_service_codes UNIQUE (municipality_ibge, service_code)
);

-- RPS has its own document series, independent of NFE/NFCE/NFSE.
ALTER TABLE document_series DROP CONSTRAINT IF EXISTS document_series_document_type_check;
ALTER TABLE document_series ADD CONSTRAINT document_series_document_type_check
    CHECK (document_type IN ('NFE', 'NFCE', 'NFSE', 'RPS'));

INSERT INTO document_series (id, company_id, document_type, series, next_number, version, created_at, modified_at)
SELECT gen_random_uuid(), c.id, 'RPS', NULL, 1, 0, NOW(), NOW()
FROM companies c
WHERE NOT EXISTS (SELECT 1 FROM document_series ds WHERE ds.company_id = c.id AND ds.document_type = 'RPS');
