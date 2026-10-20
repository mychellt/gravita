-- Semantic icon of the product (PDV, NF-e, stock). Rows with a NULL icon_key are classified on read until they are next saved.
ALTER TABLE products ADD COLUMN icon_key    VARCHAR(30);
ALTER TABLE products ADD COLUMN icon_source VARCHAR(10) NOT NULL DEFAULT 'AUTO';
