ALTER TABLE orders
    ADD COLUMN customer_name VARCHAR(50) NULL,
    ADD COLUMN customer_phone VARCHAR(20) NULL,
    ADD COLUMN customer_email VARCHAR(100) NULL;
