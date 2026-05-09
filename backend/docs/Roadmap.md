# Backend roadmap

Tài liệu mô tả bước tiếp theo cho `**backend/app**` và module `**common**` (Flyway/Repositories đi kèm).

---

## Nguyên tắc chung

- Pagination/filter: `**filters(..., Pageable)**` — tham số filter dạng `@RequestParam`, không bọc `Map` trừ khi refactor có chủ đích.
- **DELETE:** tránh hard delete nếu nghiệp vụ yêu cầu audit; hiện tại ưu tiên lifecycle/status.
- `**User`:** tách logic status tài khoản khi chỉnh sửa (`User`/admin) — không gộp sang module khác.
- **Luồng code:** Controller → Service → Repository; exception thống nhất qua lớp `*ExceptionHandle` + global handler.

---

## Thứ tự kiểm thử & hoàn thiện module API

Đã kiểm thử ổn: **User**, **Category**, **MenuItem**.


| Bước | Module          | Controller / domain | Vì sao đặt chỗ này                                                                                                  |
| ---- | --------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------- |
| 1 ✅  | User            | `/users`            | Đã xong                                                                                                             |
| 2 ✅  | Category        | `/categories`       | Đã xong                                                                                                             |
| 3 ✅  | MenuItem        | `/menu-items`       | Đã xong — catalog phụ thuộc Category                                                                                |
| 4 ✅  | **Table**       | `/tables`           | Dữ liệu bàn/ghế là nền cho reservation và order (`table_id`)                                                        |
| 5 ✅  | **Shift**       | `/shifts`           | Ca làm nhân viên (gắn User/thời gian); tách khỏi luồng order nhưng nên có trước khi đóng vòng “vận hành trong ngày” |
| 6 ✅  | **Reservation** | `/reservations`     | Khách đặt bàn — phụ thuộc Table (+ role CUSTOMER)                                                                   |
| 7 ✅  | **Order**       | `/orders`           | Đặt/xử lý đơn — thường gắn Table, user vai trò                                                                      |
| 8 ✅  | **OrderItem**   | `/order-items`      | Dòng trong đơn — phụ thuộc Order + MenuItem đã test                                                                 |
| 9 ▶️ | **Payment**     | `/payments`         | Cuối chuỗi — hoàn tiền/complete/cancel sau khi Order stable                                                         |


### Gợi ý khi test từng bước

- Sau **Table**: filter/create/update và quyền ADMIN/MANAGER (theo `@PreAuthorize` hiện tại).
- **Reservation**: luồng customer vs admin filters; chồng giờ bàn (nếu có rule nghiệp vụ).
- **Order** → **OrderItem**: tạo đơn, thêm món, đổi trạng thái; kiểm BigDecimal và message lỗi (FK/category).
- **Payment**: chỉ sau khi order/item chạy ổn; kiểm idempotency nếu có.

---

## Tài liệu liên quan

- Chi tiết DB/entity: `[database/00_README.md](./database/00_README.md)`

*(Cập nhật bảng module khi hoàn thành thêm bước.)*