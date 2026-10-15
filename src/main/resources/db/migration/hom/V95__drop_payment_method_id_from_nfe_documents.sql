-- payment_method_id was replaced by payment_method_type (V94); nothing reads or writes it any more.
ALTER TABLE nfe_documents DROP COLUMN payment_method_id;
