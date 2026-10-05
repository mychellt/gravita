-- Tenant link: which company (persons row, document = CNPJ) a customer belongs to. Nullable because customers that
-- predate multi-tenancy have no company and there is no anchor company to backfill them to; they match no
-- company's scoped list until someone assigns them one. Customers created through the API always carry one.
ALTER TABLE customers
    ADD COLUMN company_id UUID REFERENCES persons (id);

CREATE INDEX idx_customers_company_id ON customers (company_id);
