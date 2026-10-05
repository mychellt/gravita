-- Legal/trade name of the company. Existing rows have none, so they start out named after their CNPJ until someone
-- edits them.
ALTER TABLE companies ADD COLUMN name VARCHAR(255);
UPDATE companies SET name = cnpj WHERE name IS NULL;
ALTER TABLE companies ALTER COLUMN name SET NOT NULL;
