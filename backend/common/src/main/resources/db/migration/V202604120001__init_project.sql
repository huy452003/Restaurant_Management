-- PostgreSQL: schema tổng hợp (thay chuỗi migration MySQL cũ). DB mới: chạy Flyway một lần.

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    fullname VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    gender VARCHAR(50) NOT NULL,
    birth DATE NOT NULL,
    address VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    user_status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    image VARCHAR(500) NOT NULL,
    category_status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE menu_items (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    price NUMERIC(10, 2) NOT NULL,
    image VARCHAR(200) NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    menu_item_status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_menu_items_category_name FOREIGN KEY (category_name) REFERENCES categories(name)
);

CREATE TABLE tables (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    table_number INTEGER NOT NULL UNIQUE,
    capacity INTEGER,
    table_status VARCHAR(50) NOT NULL,
    location VARCHAR(200) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(50),
    customer_phone VARCHAR(20),
    customer_email VARCHAR(100),
    table_number INTEGER NOT NULL,
    waiter_id INTEGER,
    order_status VARCHAR(50) NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    sub_total NUMERIC(10, 2),
    tax NUMERIC(10, 2),
    total_amount NUMERIC(10, 2),
    notes VARCHAR(300),
    completed_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_orders_table_number FOREIGN KEY (table_number) REFERENCES tables(table_number),
    CONSTRAINT fk_orders_waiter_id FOREIGN KEY (waiter_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id INTEGER NOT NULL,
    menu_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    sub_total NUMERIC(10, 2) NOT NULL,
    special_instructions VARCHAR(100),
    order_item_status VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_menu_item_id FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id INTEGER NOT NULL,
    cashier_id INTEGER NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    paid_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_payments_order_id FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payments_cashier_id FOREIGN KEY (cashier_id) REFERENCES users(id)
);

CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(15) NOT NULL,
    customer_email VARCHAR(50) NOT NULL,
    table_number INTEGER NOT NULL,
    reservation_ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    number_of_guests INTEGER NOT NULL,
    reservation_status VARCHAR(50) NOT NULL,
    special_request VARCHAR(300),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_reservations_table_number FOREIGN KEY (table_number) REFERENCES tables(table_number)
);

CREATE TABLE shifts (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    employee_id INTEGER NOT NULL,
    shift_date DATE NOT NULL,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shift_status VARCHAR(50) NOT NULL,
    notes VARCHAR(300),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_shifts_employee_id FOREIGN KEY (employee_id) REFERENCES users(id)
);

-- Indexes (tương đương V2026051000002)
CREATE INDEX idx_users_role_status ON users(role, user_status);
CREATE INDEX idx_users_status ON users(user_status);

CREATE INDEX idx_categories_status ON categories(category_status);

CREATE INDEX idx_menu_items_category_status ON menu_items(category_name, menu_item_status);
CREATE INDEX idx_menu_items_status ON menu_items(menu_item_status);

CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_waiter_id ON orders(waiter_id);
CREATE INDEX idx_orders_table_number ON orders(table_number);
CREATE INDEX idx_orders_customer_status ON orders(customer_email, order_status);
CREATE INDEX idx_orders_waiter_status ON orders(waiter_id, order_status);
CREATE INDEX idx_orders_table_status ON orders(table_number, order_status);
CREATE INDEX idx_orders_completed_at ON orders(completed_at);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_order_id_status ON order_items(order_id, order_item_status);
CREATE INDEX idx_order_items_menu_item_id ON order_items(menu_item_id);
CREATE INDEX idx_order_items_status ON order_items(order_item_status);

CREATE INDEX idx_payments_order_status ON payments(order_id, payment_status);
CREATE INDEX idx_payments_cashier_status ON payments(cashier_id, payment_status);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);

CREATE INDEX idx_reservations_table_status_ts ON reservations(table_number, reservation_status, reservation_ts);
CREATE INDEX idx_reservations_customer_email_status ON reservations(customer_email, reservation_status);
CREATE INDEX idx_reservations_customer_phone_status ON reservations(customer_phone, reservation_status);

CREATE INDEX idx_shifts_employee_status_time ON shifts(employee_id, shift_status, start_time, end_time);
CREATE INDEX idx_shifts_employee_date ON shifts(employee_id, shift_date);
CREATE INDEX idx_shifts_status_time ON shifts(shift_status, start_time, end_time);

CREATE INDEX idx_tables_status ON tables(table_status);
CREATE INDEX idx_tables_status_capacity ON tables(table_status, capacity);
