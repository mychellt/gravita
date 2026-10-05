-- NFS-e integration data per municipality (MunicipalityIntegrationJpaEntity); previously only created by Hibernate.
CREATE TABLE IF NOT EXISTS municipality_integrations (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ibge_code                 VARCHAR(7)   NOT NULL,
    standard                  VARCHAR(20)  NOT NULL,
    version                   VARCHAR(30),
    webservice_url            VARCHAR(500),
    required_certificate_type VARCHAR(10)  NOT NULL,
    homologated               BOOLEAN      NOT NULL,
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMP    NOT NULL,
    modified_at               TIMESTAMP    NOT NULL,
    CONSTRAINT uk_municipality_integrations_ibge_code UNIQUE (ibge_code)
);

CREATE TABLE IF NOT EXISTS municipality_integration_required_fields (
    municipality_integration_id UUID         NOT NULL REFERENCES municipality_integrations (id),
    field_index                 INTEGER      NOT NULL,
    field_name                  VARCHAR(255) NOT NULL,
    PRIMARY KEY (municipality_integration_id, field_index)
);
