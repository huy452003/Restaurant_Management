ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS fk_menu_items_category_name;

ALTER TABLE menu_items
    ADD CONSTRAINT fk_menu_items_category_name
    FOREIGN KEY (category_name) REFERENCES categories (name) ON UPDATE CASCADE;
