CREATE TABLE nfe_documents (
    id                     UUID PRIMARY KEY,
    issuer_company_id      UUID          NOT NULL,
    origin_sales_order_id  UUID,
    natureza_operacao      VARCHAR(100)  NOT NULL,
    recipient_customer_id  UUID,
    recipient_document     VARCHAR(14)   NOT NULL,
    recipient_person_type  VARCHAR(20)   NOT NULL,
    recipient_name         VARCHAR(200)  NOT NULL,
    recipient_ie_indicator VARCHAR(20)   NOT NULL,
    recipient_ie           VARCHAR(14),
    recipient_state        VARCHAR(2)    NOT NULL,
    freight                NUMERIC(14, 2) NOT NULL,
    insurance              NUMERIC(14, 2) NOT NULL,
    other_expenses         NUMERIC(14, 2) NOT NULL,
    transport_modality     VARCHAR(3),
    transport_carrier      VARCHAR(200),
    transport_volume       VARCHAR(50),
    transport_gross_weight NUMERIC(14, 4),
    transport_net_weight   NUMERIC(14, 4),
    transport_rntrc        VARCHAR(20),
    referenced_access_key  VARCHAR(44),
    additional_info        TEXT,
    icms_total             NUMERIC(14, 2) NOT NULL,
    icms_st_total          NUMERIC(14, 2) NOT NULL,
    ipi_total              NUMERIC(14, 2) NOT NULL,
    pis_total              NUMERIC(14, 2) NOT NULL,
    cofins_total           NUMERIC(14, 2) NOT NULL,
    fcp_total              NUMERIC(14, 2) NOT NULL,
    grand_total            NUMERIC(14, 2) NOT NULL,
    status                 VARCHAR(20)   NOT NULL,
    series                 VARCHAR(3),
    number                 BIGINT,
    access_key             VARCHAR(44),
    protocol               VARCHAR(50),
    drafted_at             TIMESTAMP     NOT NULL,
    active                 BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP     NOT NULL,
    modified_at            TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX idx_nfe_documents_access_key ON nfe_documents (access_key) WHERE access_key IS NOT NULL;
CREATE INDEX idx_nfe_documents_issuer_company_id ON nfe_documents (issuer_company_id);

CREATE TABLE nfe_document_items (
    nfe_document_id  UUID          NOT NULL REFERENCES nfe_documents (id),
    product_id       UUID          NOT NULL,
    quantity         NUMERIC(14, 4) NOT NULL,
    unit_price       NUMERIC(14, 4) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    cfop             VARCHAR(10)   NOT NULL,
    icms_value       NUMERIC(14, 2) NOT NULL,
    icms_st_value    NUMERIC(14, 2) NOT NULL,
    ipi_value        NUMERIC(14, 2) NOT NULL,
    pis_value        NUMERIC(14, 2) NOT NULL,
    cofins_value     NUMERIC(14, 2) NOT NULL,
    fcp_value        NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_nfe_document_items_nfe_document_id ON nfe_document_items (nfe_document_id);

CREATE TABLE nfe_transmission_queue (
    id              UUID PRIMARY KEY,
    nfe_document_id UUID      NOT NULL REFERENCES nfe_documents (id),
    queued_at       TIMESTAMP NOT NULL,
    active          BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_nfe_transmission_queue_document_id ON nfe_transmission_queue (nfe_document_id);
