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
    ('00000000-0000-0000-0000-000000000001', 'Administrator', TRUE, now(), now()),
    ('00000000-0000-0000-0000-000000000002', 'Financial', TRUE, now(), now()),
    ('00000000-0000-0000-0000-000000000003', 'Salesperson', TRUE, now(), now()),
    ('00000000-0000-0000-0000-000000000004', 'Cashier Operator', TRUE, now(), now()),
    ('00000000-0000-0000-0000-000000000005', 'Purchasing', TRUE, now(), now()),
    ('00000000-0000-0000-0000-000000000006', 'Read-only', TRUE, now(), now());
