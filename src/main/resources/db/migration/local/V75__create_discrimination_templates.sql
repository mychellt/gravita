-- M4-06: reusable discrimination texts per service type (LC 116 code, canonical II.SS). Several templates may exist
-- for one service type. Documents copy the text into nfse_documents.discrimination and keep no reference to the
-- template, so a template can be edited or deleted without touching issued RPS/NFSe.
CREATE TABLE discrimination_templates (
    id            UUID PRIMARY KEY,
    service_code  VARCHAR(5) NOT NULL,
    template_text TEXT NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    modified_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_discrimination_templates_service_code ON discrimination_templates (service_code);
