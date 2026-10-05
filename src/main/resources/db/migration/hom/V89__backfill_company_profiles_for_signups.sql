-- Companies created by self-service signup before the fiscal profile existed: give each one its draft profile
-- (same id as the signup company), so Settings can load and complete it. Safe to re-run: skips existing rows.
INSERT INTO companies (id, name, cnpj, tax_regime, simples_optante, sefaz_environment, phone, active, created_at, modified_at)
SELECT p.id, p.name, p.document, 'SIMPLES_NACIONAL', FALSE, 'HOMOLOGATION', LEFT(p.phone, 20), TRUE, now(), now()
FROM persons p
WHERE p.id IN (SELECT company_id FROM users WHERE company_id IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM companies c WHERE c.id = p.id OR c.cnpj = p.document);
