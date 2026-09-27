CREATE TABLE inbound_nfe_conference_items (
    inbound_nfe_id UUID NOT NULL REFERENCES inbound_nfes (id),
    item_ref       UUID NOT NULL,
    ordered_qty    NUMERIC(14, 4) NOT NULL,
    received_qty   NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_inbound_nfe_conference_items_inbound_nfe_id ON inbound_nfe_conference_items (inbound_nfe_id);
