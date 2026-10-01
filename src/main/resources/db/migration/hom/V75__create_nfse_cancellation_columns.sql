-- M4-05: cancelling an AUTHORIZED NFSe records the mandatory justification and when the municipality confirmed it.
-- The row itself is retained - a cancelled NFSe is never deleted.
ALTER TABLE nfse_documents ADD COLUMN cancellation_justification VARCHAR(1000);
ALTER TABLE nfse_documents ADD COLUMN cancelled_at               TIMESTAMP;
