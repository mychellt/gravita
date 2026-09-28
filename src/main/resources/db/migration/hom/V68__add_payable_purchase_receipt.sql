ALTER TABLE payables ADD COLUMN purchase_receipt_ref UUID;
ALTER TABLE payables ADD COLUMN installment_number INTEGER;
ALTER TABLE payables ADD COLUMN installments INTEGER;

CREATE INDEX idx_payables_purchase_receipt_ref ON payables (purchase_receipt_ref);

-- Backs the idempotency of GeneratePayableFromReceiptUseCase: a purchase receipt has at most one
-- payable per installment, even under concurrent invocations.
CREATE UNIQUE INDEX uq_payables_purchase_receipt_installment
    ON payables (purchase_receipt_ref, installment_number);
