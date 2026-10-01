-- M4-04: transmitting a DRAFT NFSe records when it was sent, the municipality's protocol and authorization time, and a
-- reference to the transmitted XML in object storage (the XML itself is never kept in the row). A rejection returns
-- the document to DRAFT and keeps its reason until the next attempt.
ALTER TABLE nfse_documents ADD COLUMN sent_at               TIMESTAMP;
ALTER TABLE nfse_documents ADD COLUMN protocol              VARCHAR(100);
ALTER TABLE nfse_documents ADD COLUMN authorized_at         TIMESTAMP;
ALTER TABLE nfse_documents ADD COLUMN xml_reference         VARCHAR(500);
ALTER TABLE nfse_documents ADD COLUMN last_rejection_reason VARCHAR(1000);
