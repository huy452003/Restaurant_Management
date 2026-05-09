ALTER TABLE menu_items
    MODIFY COLUMN category_name VARCHAR(50) NOT NULL;

ALTER TABLE menu_items
    DROP FOREIGN KEY fk_menu_items_category_name;

ALTER TABLE menu_items
    ADD CONSTRAINT fk_menu_items_category_name
        FOREIGN KEY (category_name) REFERENCES categories(name);
