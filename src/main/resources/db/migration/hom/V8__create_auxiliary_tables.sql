CREATE TABLE payment_terms (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_term_installments (
    payment_term_id   UUID NOT NULL REFERENCES payment_terms (id),
    installment_order INT NOT NULL,
    interval_days     INT NOT NULL,
    CONSTRAINT pk_payment_term_installments PRIMARY KEY (payment_term_id, installment_order)
);

CREATE TABLE payment_methods (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(30) NOT NULL CHECK (type IN ('CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'PIX', 'BOLETO', 'STORE_CREDIT', 'VOUCHER')),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE TABLE cost_centers (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    parent_id   UUID REFERENCES cost_centers (id),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_cost_centers_code UNIQUE (code)
);

CREATE INDEX idx_cost_centers_parent_id ON cost_centers (parent_id);

CREATE TABLE chart_of_accounts (
    id           UUID PRIMARY KEY,
    code         VARCHAR(50) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    parent_id    UUID REFERENCES chart_of_accounts (id),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    modified_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_chart_of_accounts_code UNIQUE (code)
);

CREATE INDEX idx_chart_of_accounts_parent_id ON chart_of_accounts (parent_id);

CREATE TABLE ibge_municipalities (
    id          UUID PRIMARY KEY,
    ibge_code   VARCHAR(7) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    state_code  VARCHAR(2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_ibge_municipalities_ibge_code UNIQUE (ibge_code)
);

CREATE TABLE interstate_icms_rates (
    id                 UUID PRIMARY KEY,
    origin_state       VARCHAR(2) NOT NULL,
    destination_state  VARCHAR(2) NOT NULL,
    rate_percent       NUMERIC(5, 2) NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    modified_at        TIMESTAMP NOT NULL,
    CONSTRAINT uq_interstate_icms_rates_pair UNIQUE (origin_state, destination_state)
);
