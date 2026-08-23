CREATE TABLE persons (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    document          VARCHAR(20),
    email             VARCHAR(255),
    password          VARCHAR(255),
    phone             VARCHAR(20),
    representative_id UUID REFERENCES persons (id),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL,
    modified_at       TIMESTAMP NOT NULL,
    CONSTRAINT uq_persons_document UNIQUE (document),
    CONSTRAINT uq_persons_email UNIQUE (email)
);

CREATE INDEX idx_persons_representative_id ON persons (representative_id);
