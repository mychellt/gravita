-- Where a title sits in the books, for the cash-flow filters (company, branch, bank account).
ALTER TABLE receivables ADD COLUMN company_id UUID;
ALTER TABLE receivables ADD COLUMN branch_id UUID;
ALTER TABLE receivables ADD COLUMN bank_account_id UUID;

ALTER TABLE payables ADD COLUMN company_id UUID;
ALTER TABLE payables ADD COLUMN branch_id UUID;
ALTER TABLE payables ADD COLUMN bank_account_id UUID;

CREATE INDEX idx_receivables_status_due_date ON receivables (status, due_date);
CREATE INDEX idx_payables_status_due_date ON payables (status, due_date);

-- A settlement applies either to a receivable (money in) or to a payable (money out).
ALTER TABLE settlements ALTER COLUMN receivable_id DROP NOT NULL;
ALTER TABLE settlements ADD COLUMN payable_id UUID REFERENCES payables (id);
ALTER TABLE settlements ADD CONSTRAINT ck_settlements_one_title
    CHECK ((receivable_id IS NULL) <> (payable_id IS NULL));

CREATE INDEX idx_settlements_payable_id ON settlements (payable_id);
CREATE INDEX idx_settlements_settled_at ON settlements (settled_at);
