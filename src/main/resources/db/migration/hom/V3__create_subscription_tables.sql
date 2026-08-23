CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES plans (id),
    person_id       UUID NOT NULL REFERENCES persons (id),
    billing_cycle   VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAYMENT_FAILURE')),
    activation_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_subscriptions_plan_id ON subscriptions (plan_id);
CREATE INDEX idx_subscriptions_person_id ON subscriptions (person_id);

CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES subscriptions (id),
    amount          NUMERIC(10, 2) NOT NULL,
    payment_date    DATE NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);
