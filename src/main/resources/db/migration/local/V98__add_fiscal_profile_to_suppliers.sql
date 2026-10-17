-- Optional fiscal profile of a supplier, shared in shape with companies (ie/im/cnae/simples_optante). Useful when
-- entering a purchase NF-e and deciding the ICMS/PIS/COFINS credit (a Simples Nacional supplier changes it).
ALTER TABLE suppliers ADD COLUMN ie VARCHAR(20);
ALTER TABLE suppliers ADD COLUMN im VARCHAR(20);
ALTER TABLE suppliers ADD COLUMN cnae VARCHAR(10);
ALTER TABLE suppliers ADD COLUMN simples_optante BOOLEAN;
