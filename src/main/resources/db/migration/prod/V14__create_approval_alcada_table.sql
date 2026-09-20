CREATE TABLE approval_alcada (
    id                          UUID PRIMARY KEY,
    module                      VARCHAR(20) NOT NULL CHECK (module IN ('PURCHASING', 'SALES', 'FINANCE')),
    threshold_value             NUMERIC(14, 4),
    threshold_discount_percent  NUMERIC(5, 2),
    approver_profile_id         UUID NOT NULL REFERENCES profiles (id),
    configured_at               TIMESTAMP NOT NULL,
    created_at                  TIMESTAMP NOT NULL,
    modified_at                 TIMESTAMP NOT NULL
);

-- Exactly one active configuration per module (doc UC-M10-09 acceptance criteria).
CREATE UNIQUE INDEX idx_approval_alcada_module ON approval_alcada (module);
