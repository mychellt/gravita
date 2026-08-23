CREATE TABLE plans (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    tier          VARCHAR(20) NOT NULL CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD')),
    price_monthly NUMERIC(10, 2) NOT NULL,
    price_annual  NUMERIC(10, 2) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL
);

CREATE TABLE plan_features (
    plan_id UUID NOT NULL REFERENCES plans (id),
    feature VARCHAR(255) NOT NULL
);

CREATE INDEX idx_plan_features_plan_id ON plan_features (plan_id);
