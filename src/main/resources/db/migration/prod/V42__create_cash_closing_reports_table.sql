CREATE TABLE cash_closing_reports (
    id                     UUID PRIMARY KEY,
    session_id             UUID NOT NULL UNIQUE REFERENCES pos_sessions (id),
    register_id            UUID NOT NULL,
    operator_id            UUID NOT NULL,
    opening_amount         NUMERIC(14, 2) NOT NULL,
    total_sangria_amount   NUMERIC(14, 2) NOT NULL,
    total_suprimento_amount NUMERIC(14, 2) NOT NULL,
    sale_count             INTEGER NOT NULL,
    opened_at              TIMESTAMP NOT NULL,
    closed_at              TIMESTAMP NOT NULL,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP NOT NULL,
    modified_at            TIMESTAMP NOT NULL
);

CREATE TABLE cash_closing_report_expected_amounts (
    report_id      UUID NOT NULL REFERENCES cash_closing_reports (id),
    payment_method VARCHAR(20) NOT NULL,
    amount         NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_cash_closing_report_expected_amounts_report_id ON cash_closing_report_expected_amounts (report_id);

CREATE TABLE cash_closing_report_counted_amounts (
    report_id      UUID NOT NULL REFERENCES cash_closing_reports (id),
    payment_method VARCHAR(20) NOT NULL,
    amount         NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_cash_closing_report_counted_amounts_report_id ON cash_closing_report_counted_amounts (report_id);
