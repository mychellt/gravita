CREATE TABLE products (
    id                      UUID PRIMARY KEY,
    internal_code           VARCHAR(60)  NOT NULL,
    type                    VARCHAR(20)  NOT NULL CHECK (type IN ('SIMPLE', 'VARIANT', 'KIT', 'SERVICE')),
    ncm                     VARCHAR(10),
    cest                    VARCHAR(10),
    origin                  SMALLINT CHECK (origin BETWEEN 0 AND 8),
    tax_icms_rate           NUMERIC(7, 4),
    tax_ipi_rate            NUMERIC(7, 4),
    tax_pis_rate            NUMERIC(7, 4),
    tax_cofins_rate         NUMERIC(7, 4),
    tax_icms_st_rate        NUMERIC(7, 4),
    tax_fcp_rate            NUMERIC(7, 4),
    average_cost            NUMERIC(12, 2),
    base_price              NUMERIC(12, 2),
    stock_minimum           NUMERIC(12, 3),
    stock_maximum           NUMERIC(12, 3),
    stock_reorder_point     NUMERIC(12, 3),
    purchase_unit           VARCHAR(10),
    sale_unit               VARCHAR(10),
    conversion_factor       NUMERIC(12, 4),
    lot_control             BOOLEAN,
    serial_control          BOOLEAN,
    classification_group    VARCHAR(100),
    classification_subgroup VARCHAR(100),
    classification_brand    VARCHAR(100),
    classification_section  VARCHAR(100),
    status                  VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),
    active                  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP   NOT NULL,
    modified_at             TIMESTAMP   NOT NULL
);

CREATE TABLE product_barcodes (
    product_id UUID        NOT NULL REFERENCES products (id),
    barcode    VARCHAR(14) NOT NULL,
    CONSTRAINT uq_product_barcodes_barcode UNIQUE (barcode)
);

CREATE INDEX idx_product_barcodes_product_id ON product_barcodes (product_id);

CREATE TABLE product_images (
    product_id UUID         NOT NULL REFERENCES products (id),
    image_url  VARCHAR(500) NOT NULL
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

CREATE TABLE product_default_cfop_by_operation (
    product_id UUID        NOT NULL REFERENCES products (id),
    operation  VARCHAR(50) NOT NULL,
    cfop       VARCHAR(10)
);

CREATE INDEX idx_product_default_cfop_product_id ON product_default_cfop_by_operation (product_id);

CREATE TABLE product_cst_csosn_by_state (
    product_id UUID       NOT NULL REFERENCES products (id),
    state      VARCHAR(2) NOT NULL,
    cst_csosn  VARCHAR(10)
);

CREATE INDEX idx_product_cst_csosn_product_id ON product_cst_csosn_by_state (product_id);

CREATE TABLE product_kit_components (
    product_id           UUID NOT NULL REFERENCES products (id),
    component_product_id UUID NOT NULL REFERENCES products (id),
    quantity              NUMERIC(12, 3) NOT NULL
);

CREATE INDEX idx_product_kit_components_product_id ON product_kit_components (product_id);

CREATE TABLE product_variants (
    product_id UUID NOT NULL REFERENCES products (id),
    color      VARCHAR(50),
    size       VARCHAR(50),
    barcode    VARCHAR(14)
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
