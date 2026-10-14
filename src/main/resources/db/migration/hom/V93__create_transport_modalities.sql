-- GRA-224.2: Transport-Modalidade registry, the options of the NF-e form's Transporte > Modalidade select
-- (GET /api/auxiliary/transport-modalities). `code` is a TransportModality name (CIF, FOB) or NONE ("Sem transporte",
-- no modality is sent with the NF-e). Seeded with exactly those three; no further SEFAZ modFrete values.
-- Rows are deactivated, never deleted: NF-e documents keep the resolved modality in nfe_documents.transport_modality
-- and hold no reference to this table, so deactivating a row cannot alter an issued document.
CREATE TABLE transport_modalities (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(10) NOT NULL CHECK (code IN ('CIF', 'FOB', 'NONE')),
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_transport_modalities_active_code ON transport_modalities (code) WHERE active;

INSERT INTO transport_modalities (code, name) VALUES
    ('CIF',  'CIF — Por conta do emitente'),
    ('FOB',  'FOB — Por conta do destinatário'),
    ('NONE', 'Sem transporte');
