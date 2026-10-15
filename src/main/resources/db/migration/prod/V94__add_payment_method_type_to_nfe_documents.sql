-- The manual NF-e form now picks a PaymentMethodType (GET /api/auxiliary/payment-method-types) instead of a payment_methods row.
-- Informational only, like payment_method_id (kept, no longer written).
ALTER TABLE nfe_documents ADD COLUMN payment_method_type VARCHAR(20);
