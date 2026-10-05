-- One company record. "persons" held the company that signed up; "companies" holds its fiscal profile, and both
-- shared the same id. Everything that pointed at persons (subscriptions, users, customers) now points at companies,
-- and persons goes away.

-- 1. Release the old foreign keys.
ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS subscriptions_person_id_fkey;
ALTER TABLE users         DROP CONSTRAINT IF EXISTS users_company_id_fkey;
ALTER TABLE customers     DROP CONSTRAINT IF EXISTS customers_company_id_fkey;

-- 2. Any company that exists only in persons gets its draft profile (same id), so nothing is left dangling.
INSERT INTO companies (id, name, cnpj, tax_regime, simples_optante, sefaz_environment, phone, active, created_at, modified_at)
SELECT p.id, p.name, p.document, 'SIMPLES_NACIONAL', FALSE, 'HOMOLOGATION', LEFT(p.phone, 20), TRUE, now(), now()
FROM persons p
WHERE p.document IS NOT NULL
  AND p.id IN (SELECT person_id FROM subscriptions
               UNION SELECT company_id FROM users WHERE company_id IS NOT NULL
               UNION SELECT company_id FROM customers WHERE company_id IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM companies c WHERE c.id = p.id OR c.cnpj = p.document);

-- 3. A CNPJ already registered as a company under a different id: follow it.
UPDATE subscriptions s SET person_id = c.id
FROM persons p JOIN companies c ON c.cnpj = p.document
WHERE s.person_id = p.id AND c.id <> p.id;
UPDATE users u SET company_id = c.id
FROM persons p JOIN companies c ON c.cnpj = p.document
WHERE u.company_id = p.id AND c.id <> p.id;
UPDATE customers u SET company_id = c.id
FROM persons p JOIN companies c ON c.cnpj = p.document
WHERE u.company_id = p.id AND c.id <> p.id;

-- 4. Point everything at companies.
ALTER TABLE subscriptions RENAME COLUMN person_id TO company_id;
ALTER INDEX idx_subscriptions_person_id RENAME TO idx_subscriptions_company_id;
ALTER TABLE subscriptions ADD CONSTRAINT subscriptions_company_id_fkey FOREIGN KEY (company_id) REFERENCES companies (id);
ALTER TABLE users         ADD CONSTRAINT users_company_id_fkey         FOREIGN KEY (company_id) REFERENCES companies (id);
ALTER TABLE customers     ADD CONSTRAINT customers_company_id_fkey     FOREIGN KEY (company_id) REFERENCES companies (id);

-- 5. Nothing refers to persons any more.
DROP TABLE persons;
