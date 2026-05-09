-- Gộp phương thức cũ không còn dùng (CARD) về CASH trước khi app chỉ còn enum CASH/VNPAY.
UPDATE payments SET payment_method = 'CASH' WHERE payment_method = 'CARD';
