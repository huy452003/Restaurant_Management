-- Mang về gộp vào tại chỗ; giao hàng không bắt buộc bàn.
UPDATE orders SET order_type = 'DINE_IN' WHERE order_type = 'TAKE_AWAY';
ALTER TABLE orders ALTER COLUMN table_number DROP NOT NULL;
UPDATE orders SET table_number = NULL WHERE order_type = 'DELIVERY';
