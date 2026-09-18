CREATE TABLE integration_credential (
    id                           UUID PRIMARY KEY,
    integration_name             VARCHAR(40) NOT NULL CHECK (integration_name IN (
                                      'SEFAZ', 'RECEITA_FEDERAL', 'VIACEP_IBGE', 'BANK',
                                      'WHATSAPP_BUSINESS_API', 'ECOMMERCE', 'ACCOUNTING')),
    environment                  VARCHAR(20) CHECK (environment IN ('PRODUCTION', 'HOMOLOGATION')),
    endpoint                     VARCHAR(500) NOT NULL,
    encrypted_credential_payload TEXT NOT NULL,
    rotated_at                   TIMESTAMP NOT NULL,
    created_at                   TIMESTAMP NOT NULL,
    modified_at                  TIMESTAMP NOT NULL
);

-- One credential per (integration, environment); environment is only meaningful for SEFAZ
-- today, so NULL is normalized to a sentinel here to still enforce one row per integration.
CREATE UNIQUE INDEX idx_integration_credential_unique
    ON integration_credential (integration_name, COALESCE(environment, 'NONE'));
