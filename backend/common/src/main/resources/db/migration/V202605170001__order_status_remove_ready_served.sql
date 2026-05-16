-- Gộp READY/SERVED (đơn + món) sang PREPARING trước khi bỏ enum.
UPDATE orders SET order_status = 'PREPARING' WHERE order_status IN ('READY', 'SERVED');
UPDATE order_items SET order_item_status = 'PREPARING' WHERE order_item_status IN ('READY', 'SERVED');
