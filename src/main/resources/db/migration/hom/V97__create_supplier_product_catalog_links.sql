CREATE TABLE supplier_product_catalog_links (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID           NOT NULL REFERENCES companies (id),
    supplier_id              UUID           NOT NULL REFERENCES suppliers (id),
    supplier_product_code    VARCHAR(255)   NOT NULL,
    product_id               UUID           NOT NULL REFERENCES products (id),
    last_purchase_unit_value NUMERIC(14, 4) NOT NULL,
    last_purchase_at         TIMESTAMP      NOT NULL,
    created_at               TIMESTAMP      NOT NULL,
    modified_at              TIMESTAMP      NOT NULL,
    active                   BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_supplier_product_catalog_links_company_supplier_code UNIQUE (company_id, supplier_id, supplier_product_code)
);

CREATE INDEX idx_supplier_product_catalog_links_company_product ON supplier_product_catalog_links (company_id, product_id);
