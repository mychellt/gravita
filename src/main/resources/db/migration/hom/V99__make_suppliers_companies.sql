-- A supplier is a company. Its id, name, document (companies.cnpj), IE, IM, CNAE and Simples flag now live in companies;
-- suppliers (joined by id) keeps only what is specific to buying from it. A supplier is always a company, so person_type goes.

-- 1. A supplier has no SEFAZ environment, and its tax regime / Simples flag may be unknown.
ALTER TABLE companies ALTER COLUMN tax_regime        DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN sefaz_environment DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN simples_optante   DROP NOT NULL;

-- 2. One party, one CNPJ: stop here, loudly, if a supplier's document is already a company or is not a CNPJ.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM suppliers s JOIN companies c ON c.cnpj = s.document) THEN
        RAISE EXCEPTION 'A supplier document is already registered as a company; resolve it before migrating suppliers into companies';
    END IF;
    IF EXISTS (SELECT 1 FROM suppliers WHERE length(document) <> 14) THEN
        RAISE EXCEPTION 'A supplier has a non-CNPJ document; suppliers must be companies';
    END IF;
END $$;

-- 3. Every supplier becomes a company row with the same id, so every foreign key to suppliers(id) stays valid.
INSERT INTO companies (id, name, cnpj, ie, im, cnae, simples_optante, created_at, modified_at, active)
SELECT id, name, document, ie, im, cnae, simples_optante, created_at, modified_at, active
FROM suppliers;

ALTER TABLE suppliers ADD CONSTRAINT suppliers_id_fkey FOREIGN KEY (id) REFERENCES companies (id);

-- 4. What now lives in companies (or is no longer needed) leaves suppliers.
ALTER TABLE suppliers DROP COLUMN name;
ALTER TABLE suppliers DROP COLUMN document;
ALTER TABLE suppliers DROP COLUMN person_type;
ALTER TABLE suppliers DROP COLUMN ie;
ALTER TABLE suppliers DROP COLUMN im;
ALTER TABLE suppliers DROP COLUMN cnae;
ALTER TABLE suppliers DROP COLUMN simples_optante;
ALTER TABLE suppliers DROP COLUMN created_at;
ALTER TABLE suppliers DROP COLUMN modified_at;
ALTER TABLE suppliers DROP COLUMN active;
