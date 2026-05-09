ALTER TABLE reservations
    DROP FOREIGN KEY fk_reservations_table_id;

UPDATE reservations r
JOIN tables t ON r.table_number = t.id
SET r.table_number = t.table_number;

ALTER TABLE reservations
    ADD CONSTRAINT fk_reservations_table_number
    FOREIGN KEY (table_number) REFERENCES tables(table_number);
