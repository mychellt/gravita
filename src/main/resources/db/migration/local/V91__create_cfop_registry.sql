-- CFOP registry (doc §3.1, UC-M2-01 AC2): one default CFOP per NaturezaOperacao, read by the NF-e issuance use case
-- and by the GET /api/auxiliary/cfop-registry endpoint. Seeded with the values CfopRegistryAdapter used to hold in memory.
CREATE TABLE cfop_registry (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    natureza_operacao VARCHAR(30) NOT NULL,
    cfop              VARCHAR(4) NOT NULL CHECK (cfop ~ '^[0-9]{4}$'),
    description       VARCHAR(255) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cfop_registry_natureza_operacao UNIQUE (natureza_operacao),
    CONSTRAINT ck_cfop_registry_natureza_operacao
        CHECK (natureza_operacao IN ('VENDA', 'DEVOLUCAO', 'NOTA_COMPLEMENTAR', 'TRANSFERENCIA', 'REMESSA', 'OUTRA'))
);

INSERT INTO cfop_registry (natureza_operacao, cfop, description) VALUES
    ('VENDA',             '5102', 'Venda de mercadoria'),
    ('DEVOLUCAO',         '5202', 'Devolução'),
    ('NOTA_COMPLEMENTAR', '5124', 'Nota complementar'),
    ('TRANSFERENCIA',     '5152', 'Transferência'),
    ('REMESSA',           '5915', 'Remessa'),
    ('OUTRA',             '5949', 'Outra saída');
