CREATE TABLE xml_objects (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies (id),
    content     BYTEA NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE inbound_nfes (
    id                   UUID PRIMARY KEY,
    company_id           UUID NOT NULL REFERENCES companies (id),
    access_key           VARCHAR(44) NOT NULL,
    series               VARCHAR(3) NOT NULL,
    number               VARCHAR(9) NOT NULL,
    supplier_document    VARCHAR(14) NOT NULL,
    supplier_name        TEXT NOT NULL,
    issued_at            TIMESTAMP NOT NULL,
    products_value       NUMERIC(14, 4) NOT NULL,
    freight_value        NUMERIC(14, 4) NOT NULL,
    insurance_value      NUMERIC(14, 4) NOT NULL,
    discount_value       NUMERIC(14, 4) NOT NULL,
    other_expenses_value NUMERIC(14, 4) NOT NULL,
    icms_value           NUMERIC(14, 4) NOT NULL,
    ipi_value            NUMERIC(14, 4) NOT NULL,
    pis_value            NUMERIC(14, 4) NOT NULL,
    cofins_value         NUMERIC(14, 4) NOT NULL,
    total_value          NUMERIC(14, 4) NOT NULL,
    xml_storage_ref       TEXT NOT NULL,
    status               VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_CONFERENCE', 'CONFIRMED')),
    imported_at          TIMESTAMP NOT NULL,
    created_at           TIMESTAMP NOT NULL,
    modified_at          TIMESTAMP NOT NULL,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_inbound_nfes_access_key UNIQUE (access_key)
);

CREATE TABLE inbound_nfe_items (
    inbound_nfe_id       UUID NOT NULL REFERENCES inbound_nfes (id),
    supplier_product_code TEXT NOT NULL,
    description          TEXT NOT NULL,
    ncm                  VARCHAR(8),
    cfop                 VARCHAR(4),
    unit                 VARCHAR(6),
    quantity             NUMERIC(14, 4) NOT NULL,
    unit_value           NUMERIC(14, 4) NOT NULL,
    total_value          NUMERIC(14, 4) NOT NULL,
    icms_value           NUMERIC(14, 4) NOT NULL,
    ipi_value            NUMERIC(14, 4) NOT NULL,
    pis_value            NUMERIC(14, 4) NOT NULL,
    cofins_value         NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_inbound_nfe_items_inbound_nfe_id ON inbound_nfe_items (inbound_nfe_id);
CREATE INDEX idx_inbound_nfes_company_id ON inbound_nfes (company_id);
CREATE INDEX idx_xml_objects_company_id ON xml_objects (company_id);
