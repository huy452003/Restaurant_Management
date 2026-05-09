-- users
CREATE INDEX idx_users_role_status ON users(role, user_status);
CREATE INDEX idx_users_status ON users(user_status);

-- categories
CREATE INDEX idx_categories_status ON categories(category_status);

-- menu_items
CREATE INDEX idx_menu_items_category_status ON menu_items(category_name, menu_item_status);
CREATE INDEX idx_menu_items_status ON menu_items(menu_item_status);

-- orders
CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_waiter_id ON orders(waiter_id);
CREATE INDEX idx_orders_table_number ON orders(table_number);
CREATE INDEX idx_orders_customer_status ON orders(customer_email, order_status);
CREATE INDEX idx_orders_waiter_status ON orders(waiter_id, order_status);
CREATE INDEX idx_orders_table_status ON orders(table_number, order_status);
CREATE INDEX idx_orders_completed_at ON orders(completed_at);

-- order_items
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_order_id_status ON order_items(order_id, order_item_status);
CREATE INDEX idx_order_items_menu_item_id ON order_items(menu_item_id);
CREATE INDEX idx_order_items_status ON order_items(order_item_status);

-- payments
CREATE INDEX idx_payments_order_status ON payments(order_id, payment_status);
CREATE INDEX idx_payments_cashier_status ON payments(cashier_id, payment_status);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);

-- reservations
CREATE INDEX idx_reservations_table_status_ts ON reservations(table_number, reservation_status, reservation_ts);
CREATE INDEX idx_reservations_customer_email_status ON reservations(customer_email, reservation_status);
CREATE INDEX idx_reservations_customer_phone_status ON reservations(customer_phone, reservation_status);

-- shifts
CREATE INDEX idx_shifts_employee_status_time ON shifts(employee_id, shift_status, start_time, end_time);
CREATE INDEX idx_shifts_employee_date ON shifts(employee_id, shift_date);
CREATE INDEX idx_shifts_status_time ON shifts(shift_status, start_time, end_time);

-- tables
CREATE INDEX idx_tables_status ON tables(table_status);
CREATE INDEX idx_tables_status_capacity ON tables(table_status, capacity);
