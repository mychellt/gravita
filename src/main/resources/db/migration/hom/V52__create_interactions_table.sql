CREATE TABLE interactions (
    id             UUID PRIMARY KEY,
    opportunity_id UUID REFERENCES opportunities (id),
    customer_id    UUID,
    channel        VARCHAR(20) NOT NULL
        CHECK (channel IN ('CALL', 'VISIT', 'EMAIL', 'WHATSAPP')),
    summary        VARCHAR(1000) NOT NULL,
    "timestamp"    TIMESTAMP NOT NULL,
    CONSTRAINT chk_interactions_has_target CHECK (opportunity_id IS NOT NULL OR customer_id IS NOT NULL)
);

CREATE INDEX idx_interactions_opportunity_id ON interactions (opportunity_id);
CREATE INDEX idx_interactions_customer_id ON interactions (customer_id);
