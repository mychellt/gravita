CREATE TABLE quotations (
    id             UUID PRIMARY KEY,
    request_id     UUID NOT NULL REFERENCES purchase_requests (id),
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL
);

CREATE TABLE quotation_items (
    quotation_id   UUID NOT NULL REFERENCES quotations (id),
    product_id     UUID NOT NULL,
    quantity       NUMERIC(14, 4) NOT NULL
);

CREATE TABLE quotation_suppliers (
    quotation_id   UUID NOT NULL REFERENCES quotations (id),
    supplier_id    UUID NOT NULL
);

CREATE TABLE quotation_response_lines (
    quotation_id   UUID NOT NULL REFERENCES quotations (id),
    supplier_id    UUID NOT NULL,
    deadline       DATE NOT NULL,
    product_id     UUID NOT NULL,
    unit_price     NUMERIC(14, 4) NOT NULL
);

CREATE INDEX idx_quotation_items_quotation_id ON quotation_items (quotation_id);
CREATE INDEX idx_quotation_suppliers_quotation_id ON quotation_suppliers (quotation_id);
CREATE INDEX idx_quotation_response_lines_quotation_id ON quotation_response_lines (quotation_id);
CREATE INDEX idx_quotations_request_id ON quotations (request_id);
