CREATE TABLE nfe_documents (
    id                            UUID PRIMARY KEY,
    issuer_company_id             UUID NOT NULL,
    origin_sales_order_id         UUID,
    natureza_operacao             VARCHAR(30) NOT NULL,
    cfop                          VARCHAR(4) NOT NULL,
    recipient_person_id           UUID,
    recipient_document            VARCHAR(14) NOT NULL,
    recipient_person_type         VARCHAR(20) NOT NULL,
    recipient_name                VARCHAR(255) NOT NULL,
    recipient_state_registration  VARCHAR(20),
    recipient_state               VARCHAR(2) NOT NULL,
    freight                       NUMERIC(14, 2) NOT NULL,
    insurance                     NUMERIC(14, 2) NOT NULL,
    other_expenses                NUMERIC(14, 2) NOT NULL,
    transport_modality            VARCHAR(10),
    transport_carrier             VARCHAR(255),
    transport_volume              INTEGER,
    transport_gross_weight        NUMERIC(14, 4),
    transport_net_weight          NUMERIC(14, 4),
    transport_rntrc               VARCHAR(20),
    referenced_access_key         VARCHAR(44),
    additional_info               TEXT,
    status                        VARCHAR(20) NOT NULL,
    document_created_at           TIMESTAMP NOT NULL,
    document_series               VARCHAR(3),
    document_number               BIGINT,
    access_key                    VARCHAR(44),
    sefaz_protocol                VARCHAR(255),
    active                        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMP NOT NULL,
    modified_at                   TIMESTAMP NOT NULL
);

CREATE INDEX idx_nfe_documents_issuer_company_id ON nfe_documents (issuer_company_id);
CREATE INDEX idx_nfe_documents_origin_sales_order_id ON nfe_documents (origin_sales_order_id);

CREATE TABLE nfe_items (
    nfe_document_id UUID NOT NULL REFERENCES nfe_documents (id),
    item_index      INTEGER NOT NULL,
    product_id      UUID NOT NULL,
    description     VARCHAR(255),
    quantity        NUMERIC(14, 4) NOT NULL,
    unit_price      NUMERIC(14, 4) NOT NULL,
    discount        NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_nfe_items_nfe_document_id ON nfe_items (nfe_document_id);

CREATE TABLE nfe_item_tax_lines (
    nfe_document_id     UUID NOT NULL REFERENCES nfe_documents (id),
    item_index          INTEGER NOT NULL,
    tax_type            VARCHAR(20) NOT NULL,
    base                NUMERIC(14, 2) NOT NULL,
    rate_percentage     NUMERIC(7, 4) NOT NULL,
    computed_amount     NUMERIC(14, 2) NOT NULL,
    final_amount        NUMERIC(14, 2) NOT NULL,
    overridden          BOOLEAN NOT NULL DEFAULT FALSE,
    override_justification VARCHAR(500)
);

CREATE INDEX idx_nfe_item_tax_lines_nfe_document_id ON nfe_item_tax_lines (nfe_document_id);
