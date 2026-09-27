ALTER TABLE quotes ADD COLUMN salesperson_id UUID;
UPDATE quotes SET salesperson_id = '00000000-0000-0000-0000-000000000000' WHERE salesperson_id IS NULL;
ALTER TABLE quotes ALTER COLUMN salesperson_id SET NOT NULL;

ALTER TABLE sales_orders ADD COLUMN salesperson_id UUID;
UPDATE sales_orders SET salesperson_id = '00000000-0000-0000-0000-000000000000' WHERE salesperson_id IS NULL;
ALTER TABLE sales_orders ALTER COLUMN salesperson_id SET NOT NULL;
ALTER TABLE sales_orders ADD COLUMN invoiced_at DATE;

CREATE INDEX idx_quotes_salesperson_id ON quotes (salesperson_id);
CREATE INDEX idx_sales_orders_salesperson_id ON sales_orders (salesperson_id);
