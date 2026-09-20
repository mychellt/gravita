CREATE TABLE suppliers (
    id                     UUID PRIMARY KEY,
    document               VARCHAR(20) NOT NULL,
    person_type            VARCHAR(20) NOT NULL CHECK (person_type IN ('INDIVIDUAL', 'COMPANY')),
    name                   VARCHAR(255) NOT NULL,
    bank_code              VARCHAR(10),
    bank_agency            VARCHAR(20),
    bank_account_number    VARCHAR(30),
    pix_key                VARCHAR(255),
    average_lead_time_days INTEGER,
    default_purchase_cfop  VARCHAR(4),
    created_at             TIMESTAMP NOT NULL,
    modified_at            TIMESTAMP NOT NULL,
    CONSTRAINT uq_suppliers_document UNIQUE (document)
);

CREATE TABLE supplier_addresses (
    supplier_id  UUID NOT NULL REFERENCES suppliers (id),
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(20),
    complement   VARCHAR(255),
    neighborhood VARCHAR(255) NOT NULL,
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(2) NOT NULL,
    zip_code     VARCHAR(10) NOT NULL
);

CREATE INDEX idx_supplier_addresses_supplier_id ON supplier_addresses (supplier_id);

CREATE TABLE supplier_contacts (
    supplier_id UUID NOT NULL REFERENCES suppliers (id),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('EMAIL', 'PHONE', 'WHATSAPP')),
    contact_value VARCHAR(255) NOT NULL
);

CREATE INDEX idx_supplier_contacts_supplier_id ON supplier_contacts (supplier_id);
