-- Tenant link: which company (persons row, document = CNPJ) a user belongs to. Nullable because users that
-- predate self-service signup have no company; users created by signup always carry one.
ALTER TABLE users
    ADD COLUMN company_id UUID REFERENCES persons (id);

CREATE INDEX idx_users_company_id ON users (company_id);
