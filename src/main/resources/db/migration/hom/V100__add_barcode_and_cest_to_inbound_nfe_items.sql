-- GTIN (cEAN) and CEST as the supplier's XML states them; both optional, used to prefill the product registration.
ALTER TABLE inbound_nfe_items ADD COLUMN barcode VARCHAR(14);
ALTER TABLE inbound_nfe_items ADD COLUMN cest    VARCHAR(7);
