CREATE TABLE profiles (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE TABLE profile_permissions (
    profile_id UUID NOT NULL REFERENCES profiles (id),
    module     VARCHAR(255) NOT NULL,
    screen     VARCHAR(255) NOT NULL,
    action     VARCHAR(20) NOT NULL CHECK (action IN ('VIEW', 'CREATE', 'EDIT', 'DELETE', 'APPROVE', 'EXPORT'))
);

CREATE INDEX idx_profile_permissions_profile_id ON profile_permissions (profile_id);

INSERT INTO profiles (id, name, active, created_at, modified_at) VALUES
    (gen_random_uuid(), 'Administrator', TRUE, now(), now()),
    (gen_random_uuid(), 'Financial', TRUE, now(), now()),
    (gen_random_uuid(), 'Salesperson', TRUE, now(), now()),
    (gen_random_uuid(), 'Cashier Operator', TRUE, now(), now()),
    (gen_random_uuid(), 'Purchasing', TRUE, now(), now()),
    (gen_random_uuid(), 'Read-only', TRUE, now(), now());
