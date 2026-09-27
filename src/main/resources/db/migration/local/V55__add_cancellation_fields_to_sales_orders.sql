ALTER TABLE sales_orders
    ADD COLUMN cancel_reason VARCHAR(500);

CREATE TABLE sales_order_stock_reservations (
    sales_order_id UUID NOT NULL REFERENCES sales_orders (id),
    reservation_id UUID NOT NULL
);

CREATE INDEX idx_sales_order_stock_reservations_sales_order_id ON sales_order_stock_reservations (sales_order_id);
