-- Plan description, highlight flag, usage limits (NULL = unlimited), support terms and richer feature rows.
-- Existing plans keep working: they get an empty description, unlimited usage and a basic e-mail support
-- terms (24h SLA, business hours) until an admin edits them.
ALTER TABLE plans
    ADD COLUMN description               VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN featured                  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN limit_cnpjs               INTEGER CHECK (limit_cnpjs >= 1),
    ADD COLUMN limit_filiais             INTEGER CHECK (limit_filiais >= 1),
    ADD COLUMN limit_caixas_pdv          INTEGER CHECK (limit_caixas_pdv >= 1),
    ADD COLUMN limit_usuarios            INTEGER CHECK (limit_usuarios >= 1),
    ADD COLUMN support_email             BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN support_chat              BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN support_telefone          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN support_sla_horas         INTEGER NOT NULL DEFAULT 24 CHECK (support_sla_horas BETWEEN 1 AND 72),
    ADD COLUMN support_horario_comercial BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN support_gerente_dedicado  BOOLEAN NOT NULL DEFAULT FALSE;

-- plan_features: (plan_id, feature) -> (plan_id, label, included, display_order).
-- Every existing feature becomes an included row, ordered as it is currently stored.
ALTER TABLE plan_features
    ADD COLUMN label         VARCHAR(255),
    ADD COLUMN included      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN display_order INTEGER;

UPDATE plan_features pf
SET label         = pf.feature,
    display_order = ordered.position
FROM (SELECT ctid AS row_id,
             ROW_NUMBER() OVER (PARTITION BY plan_id ORDER BY ctid) - 1 AS position
      FROM plan_features) ordered
WHERE pf.ctid = ordered.row_id;

ALTER TABLE plan_features
    ALTER COLUMN label SET NOT NULL,
    ALTER COLUMN display_order SET NOT NULL,
    ALTER COLUMN included DROP DEFAULT,
    DROP COLUMN feature;
