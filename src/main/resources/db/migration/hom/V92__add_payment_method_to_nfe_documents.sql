-- GRA-226: payment method chosen on the manual NF-e form (GET /api/auxiliary/payment-methods). Informational only:
-- it is stored and returned with the document but is not rendered into the NF-e XML (no cobranca/duplicata block).
ALTER TABLE nfe_documents ADD COLUMN payment_method_id UUID;
