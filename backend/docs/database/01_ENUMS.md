# Enums - Hệ Thống Quản Lý Nhà Hàng

Tất cả các enums được định nghĩa trong module `common` tại package `com.enums`.

## 1. UserRole
Vai trò người dùng trong hệ thống
- `ADMIN` - Quản trị viên
- `MANAGER` - Quản lý
- `WAITER` - Nhân viên phục vụ
- `CHEF` - Đầu bếp
- `CASHIER` - Thu ngân

## 2. UserStatus
Trạng thái tài khoản người dùng
- `ACTIVE` - Đang hoạt động
- `INACTIVE` - Không hoạt động

## 3. OrderStatus
Trạng thái đơn hàng
- `PENDING` - Chờ xử lý
- `CONFIRMED` - Đã xác nhận
- `PREPARING` - Đang chuẩn bị
- `READY` - Sẵn sàng phục vụ
- `SERVED` - Đã phục vụ
- `COMPLETED` - Hoàn thành
- `CANCELLED` - Đã hủy

## 4. OrderType
Loại đơn hàng
- `DINE_IN` - Ăn tại chỗ
- `TAKEAWAY` - Mang đi
- `DELIVERY` - Giao hàng

## 5. OrderItemStatus
Trạng thái món trong đơn
- `PENDING` - Chờ xử lý
- `PREPARING` - Đang chuẩn bị
- `READY` - Sẵn sàng
- `SERVED` - Đã phục vụ

## 6. TableStatus
Trạng thái bàn ăn
- `AVAILABLE` - Có sẵn
- `OCCUPIED` - Đang sử dụng
- `RESERVED` - Đã đặt trước
- `CLEANING` - Đang dọn dẹp

## 7. MenuItemStatus
Trạng thái món ăn
- `AVAILABLE` - Có sẵn
- `OUT_OF_STOCK` - Hết hàng
- `DISCONTINUED` - Ngừng phục vụ

## 8. PaymentMethod
Phương thức thanh toán
- `CASH` - Tiền mặt
- `CARD` - Thẻ
- `BANK_TRANSFER` - Chuyển khoản
- `E_WALLET` - Ví điện tử

## 9. PaymentStatus
Trạng thái thanh toán
- `PENDING` - Chờ thanh toán
- `COMPLETED` - Đã thanh toán
- `REFUNDED` - Đã hoàn tiền

## 10. ShiftStatus
Trạng thái ca làm việc
- `SCHEDULED` - Đã lên lịch
- `IN_PROGRESS` - Đang làm việc
- `COMPLETED` - Đã hoàn thành
- `CANCELLED` - Đã hủy

## 11. ReservationStatus
Trạng thái đặt bàn
- `PENDING` - Chờ xác nhận
- `CONFIRMED` - Đã xác nhận
- `SEATED` - Đã ngồi
- `CANCELLED` - Đã hủy
- `NO_SHOW` - Không đến

## 12. DiscountType
Loại giảm giá
- `PERCENTAGE` - Giảm theo phần trăm
- `FIXED_AMOUNT` - Giảm số tiền cố định

## 13. PromotionStatus
Trạng thái khuyến mãi
- `ACTIVE` - Đang hoạt động
- `INACTIVE` - Không hoạt động
- `EXPIRED` - Đã hết hạn

## 14. InventoryStatus
Trạng thái kho hàng
- `IN_STOCK` - Còn hàng
- `LOW_STOCK` - Sắp hết hàng
- `OUT_OF_STOCK` - Hết hàng
