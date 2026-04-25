# Các Trường Quan Trọng Cần Lưu Ý

## 1. ORDER.orderNumber
- **Format**: "ORD-YYYYMMDD-XXXX" (ví dụ: ORD-20241222-0001)
- Tự động generate, unique
- Được dùng để tìm kiếm và tham chiếu đơn hàng

## 2. ORDER.totalAmount
- **Tính toán**: `subTotal + tax - discount`
- Validate: `totalAmount >= 0`
- Lưu ý: Cần tính toán lại khi có thay đổi về discount hoặc tax

## 3. ORDER_ITEM.unitPrice
- **Lưu giá tại thời điểm đặt hàng**
- Không thay đổi dù `MenuItem.price` thay đổi sau đó
- Đảm bảo tính nhất quán của đơn hàng trong quá khứ

## 4. PAYMENT.amount
- **Có thể thanh toán nhiều lần cho 1 Order**
- Tổng `Payment.amount` có thể `<= Order.totalAmount`
- Hỗ trợ thanh toán từng phần (partial payment)

## 5. TABLE.status
- **Tự động update**:
  - `OCCUPIED` khi có Order mới
  - `AVAILABLE` khi Order hoàn thành
- Cần cập nhật qua business logic, không nên để client tự update

## 6. MENU_ITEM.status
- `OUT_OF_STOCK`: Không cho phép thêm vào Order
- `DISCONTINUED`: Ẩn khỏi menu, nhưng vẫn hiển thị trong Order cũ
- Cần validate trước khi thêm vào Order

## 7. PROMOTION.applicableItems
- Nếu `null` hoặc empty: Áp dụng cho tất cả MenuItem
- Nếu có giá trị: Chỉ áp dụng cho các MenuItem được chọn

## 8. RESERVATION và TABLE
- Cần validate: `numberOfGuests <= Table.capacity`
- `reservationDate` và `reservationTime` không được trong quá khứ
- Kiểm tra xung đột khi đặt bàn (table đã reserved hoặc occupied)
