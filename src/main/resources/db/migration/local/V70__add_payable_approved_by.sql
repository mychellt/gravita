-- Who approved the payable (UC-M8-12); null until it leaves OPEN.
ALTER TABLE payables ADD COLUMN approved_by UUID;
