-- Tránh hai đặt bàn PENDING/CONFIRMED trùng (bàn + ngày + khung giờ) khi request đồng thời.
CREATE UNIQUE INDEX ux_reservations_active_slot
    ON reservations (table_number, reservation_date, reservation_time)
    WHERE reservation_status IN ('PENDING', 'CONFIRMED');
