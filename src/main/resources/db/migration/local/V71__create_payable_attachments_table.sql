-- References (URLs) to the documents linked to a payable, e.g. the PIX payment receipt (UC-M8-14), in the order added.
CREATE TABLE payable_attachments (
    payable_id UUID NOT NULL REFERENCES payables (id),
    position   INTEGER NOT NULL,
    url        VARCHAR(2048) NOT NULL,
    PRIMARY KEY (payable_id, position)
);
