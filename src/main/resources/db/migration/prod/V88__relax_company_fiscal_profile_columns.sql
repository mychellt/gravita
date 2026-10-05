-- A company is created at signup with only name, CNPJ and phone; the fiscal profile is filled in later in Settings.
ALTER TABLE companies ALTER COLUMN ie            DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN im            DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN cnae          DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN address       DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN issuing_email DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN phone         DROP NOT NULL;
