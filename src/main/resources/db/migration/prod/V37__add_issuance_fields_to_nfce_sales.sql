ALTER TABLE nfce_sales
    ADD COLUMN document_series  VARCHAR(3),
    ADD COLUMN document_number  BIGINT,
    ADD COLUMN access_key       VARCHAR(44),
    ADD COLUMN sefaz_protocol   VARCHAR(50),
    ADD COLUMN contingency_mode BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX idx_nfce_sales_access_key ON nfce_sales (access_key) WHERE access_key IS NOT NULL;
