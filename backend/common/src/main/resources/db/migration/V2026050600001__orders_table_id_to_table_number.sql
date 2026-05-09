ALTER TABLE orders
    DROP FOREIGN KEY fk_orders_table_id;

ALTER TABLE orders
    RENAME COLUMN table_id TO table_number;

UPDATE orders o
JOIN tables t ON o.table_number = t.id
SET o.table_number = t.table_number;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_table_number
    FOREIGN KEY (table_number) REFERENCES tables(table_number);
