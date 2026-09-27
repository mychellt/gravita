CREATE TABLE opportunities (
    id                   UUID PRIMARY KEY,
    customer_id          UUID NOT NULL,
    estimated_value      NUMERIC(14, 2) NOT NULL,
    probability          INTEGER NOT NULL,
    expected_close_date  DATE NOT NULL,
    owner                UUID NOT NULL,
    stage                VARCHAR(20) NOT NULL
        CHECK (stage IN ('PROSPECTING', 'PROPOSAL', 'NEGOTIATION', 'CLOSED', 'LOST')),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    modified_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_opportunities_customer_id ON opportunities (customer_id);
CREATE INDEX idx_opportunities_owner ON opportunities (owner);
CREATE INDEX idx_opportunities_stage ON opportunities (stage);

CREATE TABLE opportunity_stage_transitions (
    id             UUID PRIMARY KEY,
    opportunity_id UUID NOT NULL REFERENCES opportunities (id),
    from_stage     VARCHAR(20) NOT NULL
        CHECK (from_stage IN ('PROSPECTING', 'PROPOSAL', 'NEGOTIATION', 'CLOSED', 'LOST')),
    to_stage       VARCHAR(20) NOT NULL
        CHECK (to_stage IN ('PROSPECTING', 'PROPOSAL', 'NEGOTIATION', 'CLOSED', 'LOST')),
    "timestamp"    TIMESTAMP NOT NULL
);

CREATE INDEX idx_opportunity_stage_transitions_opportunity_id ON opportunity_stage_transitions (opportunity_id);
