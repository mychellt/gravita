CREATE TABLE customers (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    person_type     VARCHAR(20) NOT NULL CHECK (person_type IN ('INDIVIDUAL', 'COMPANY')),
    document        VARCHAR(20) NOT NULL,
    email           VARCHAR(255),
    ie_indicator    VARCHAR(20) CHECK (ie_indicator IN ('TAXPAYER', 'EXEMPT', 'NON_TAXPAYER')),
    final_consumer  BOOLEAN,
    credit_limit    NUMERIC(14, 2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('REGULAR', 'BLOCKED', 'DELINQUENT')),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL,
    CONSTRAINT uq_customers_document UNIQUE (document)
);

CREATE TABLE customer_addresses (
    customer_id  UUID NOT NULL REFERENCES customers (id),
    type         VARCHAR(20) NOT NULL CHECK (type IN ('BILLING', 'DELIVERY')),
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(20),
    complement   VARCHAR(255),
    neighborhood VARCHAR(255) NOT NULL,
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(2) NOT NULL,
    zip_code     VARCHAR(10) NOT NULL,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses (customer_id);

CREATE TABLE customer_contacts (
    customer_id UUID NOT NULL REFERENCES customers (id),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('EMAIL', 'WHATSAPP', 'PHONE')),
    contact_value VARCHAR(255) NOT NULL
);

CREATE INDEX idx_customer_contacts_customer_id ON customer_contacts (customer_id);

CREATE TABLE customer_price_tables (
    customer_id    UUID NOT NULL REFERENCES customers (id),
    price_table_id UUID NOT NULL,
    priority       INTEGER NOT NULL
);

CREATE INDEX idx_customer_price_tables_customer_id ON customer_price_tables (customer_id);
