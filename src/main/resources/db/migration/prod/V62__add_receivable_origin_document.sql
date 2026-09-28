ALTER TABLE receivables ADD COLUMN origin_document_ref UUID;
ALTER TABLE receivables ADD COLUMN installment_number INTEGER;

CREATE INDEX idx_receivables_origin_document_ref ON receivables (origin_document_ref);

-- Backs the idempotency of GenerateReceivableFromInvoicingUseCase: a fiscal document has at most one
-- receivable per installment, even under concurrent invocations.
CREATE UNIQUE INDEX uq_receivables_origin_document_installment
    ON receivables (origin_document_ref, installment_number);
