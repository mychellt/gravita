-- Default plan catalog (Bronze, Silver, Gold) shown on /admin/settings/planos and the public pricing page.
-- Only seeds a catalog that is still empty: an environment where plans were already created keeps what it has.
-- Limits are NULL where unlimited. Feature rows are ordered by display_order, as the admin screen lists them.
INSERT INTO plans (id, name, tier, description, price_monthly, price_annual, active, featured,
                   limit_cnpjs, limit_filiais, limit_caixas_pdv, limit_usuarios,
                   support_email, support_chat, support_telefone, support_sla_horas,
                   support_horario_comercial, support_gerente_dedicado, created_at, modified_at)
SELECT seed.*, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Bronze', 'BRONZE',
     'Para pequenos varejos com até 1 CNPJ e operação simplificada.',
     297.00, 247.00, TRUE, FALSE, 1, 1, 1, 3, TRUE, TRUE, FALSE, 8, TRUE, FALSE),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'Silver', 'SILVER',
     'Para varejos em crescimento com múltiplos caixas, NFS-e e integrações.',
     597.00, 497.00, TRUE, TRUE, 1, 3, 5, 10, TRUE, TRUE, FALSE, 8, TRUE, FALSE),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'Gold', 'GOLD',
     'Para redes de varejo e operações complexas com múltiplos CNPJs e API aberta.',
     1197.00, 997.00, TRUE, FALSE, NULL::integer, NULL::integer, NULL::integer, NULL::integer,
     TRUE, TRUE, TRUE, 2, FALSE, TRUE)
) AS seed (id, name, tier, description, price_monthly, price_annual, active, featured,
           limit_cnpjs, limit_filiais, limit_caixas_pdv, limit_usuarios,
           support_email, support_chat, support_telefone, support_sla_horas,
           support_horario_comercial, support_gerente_dedicado)
WHERE NOT EXISTS (SELECT 1 FROM plans);

INSERT INTO plan_features (plan_id, label, included, display_order)
SELECT seed.plan_id, seed.label, seed.included, seed.display_order
FROM (VALUES
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'NF-e e NFC-e ilimitadas',          TRUE,  0),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Estoque e Compras',                TRUE,  1),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Financeiro básico (CR/CP)',        TRUE,  2),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Dashboard executivo',              TRUE,  3),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'NFS-e (serviços)',                 FALSE, 4),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Integração bancária (boleto/PIX)', FALSE, 5),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'Integração e-commerce',            FALSE, 6),
    ('8f3a83e9-5fe9-4743-b588-bb60c28d8b23'::uuid, 'API REST + Webhooks',              FALSE, 7),

    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'NF-e, NFC-e e NFS-e ilimitadas',   TRUE,  0),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'Estoque, Compras e CRM completo',  TRUE,  1),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'Financeiro + integração bancária', TRUE,  2),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'NFS-e multi-município',            TRUE,  3),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'WhatsApp Business API',            TRUE,  4),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'Relatórios avançados + DRE',       TRUE,  5),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'Conciliação bancária (OFX/CSV)',   TRUE,  6),
    ('62730fce-4130-4541-8874-0ece8ed39f9a'::uuid, 'API REST + Webhooks',              FALSE, 7),

    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'NF-e, NFC-e e NFS-e ilimitadas',        TRUE, 0),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'Todos os módulos incluídos',            TRUE, 1),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'API REST + Webhooks',                   TRUE, 2),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'Integração e-commerce (Shopify, VTEX)', TRUE, 3),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'Ambiente sandbox incluso',              TRUE, 4),
    ('bf3ca505-57b7-41fd-b972-93e5812fdc6e'::uuid, 'Exportação contábil configurável',      TRUE, 5)
) AS seed (plan_id, label, included, display_order)
WHERE EXISTS (SELECT 1 FROM plans WHERE plans.id = seed.plan_id)
  AND NOT EXISTS (SELECT 1 FROM plan_features WHERE plan_features.plan_id = seed.plan_id);
