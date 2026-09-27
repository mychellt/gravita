CREATE TABLE inbound_manifestations (
    id             UUID PRIMARY KEY,
    access_key     VARCHAR(44) NOT NULL,
    type           VARCHAR(30) NOT NULL,
    inbound_nfe_id UUID,
    sefaz_protocol VARCHAR(255) NOT NULL,
    manifested_at  TIMESTAMP NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_inbound_manifestations_access_key ON inbound_manifestations (access_key);
