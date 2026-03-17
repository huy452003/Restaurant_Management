# Backend Dev Roadmap (Practice)

## Thứ tự module nên làm

1. Menu
   - Category: CRUD, filter/paginate
   - MenuItem: CRUD, filter/paginate, status (ACTIVE/INACTIVE)

2. Tables & Reservations
   - Table: CRUD, thay đổi trạng thái bàn
   - Reservation: đặt bàn, hủy đặt bàn, tránh trùng giờ

3. Orders & Payments
   - Order: tạo order cho bàn, thêm/sửa/xóa món
   - OrderItem: line items trong order
   - Payment: thanh toán, nhiều phương thức, trạng thái paid/unpaid

4. Inventory
   - Inventory item: CRUD, tồn kho, min stock
   - Nhập kho, xuất kho, không cho xuất vượt tồn

5. Others (khi rảnh luyện thêm)
   - Promotions: khuyến mãi theo món hoặc theo bill
   - Shifts: ca làm việc cho nhân viên
   - Reviews: đánh giá, phản hồi khách
   - Reports: thống kê doanh thu, món bán chạy, tồn kho