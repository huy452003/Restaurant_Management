# Design Guidelines & Best Practices

## 1. BASE ENTITY (Tùy chọn)
Tạo abstract class `BaseEntity` với các trường:
- `id`: Long
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime

Các entity khác extends `BaseEntity` để tránh lặp code.

## 2. VALIDATION
- Dùng `@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Email`, `@DecimalMin` cho các trường quan trọng
- Validate business logic trong Service layer

## 3. CASCADE
- `Order` → `OrderItem`: `CascadeType.ALL` (khi xóa Order thì xóa OrderItem)
- `Order` → `Payment`: Không cascade (Payment có thể tồn tại độc lập)
- `Category` → `MenuItem`: Không cascade (MenuItem không nên bị xóa khi xóa Category)

## 4. FETCH TYPE
- Tất cả `@ManyToOne`: `FetchType.LAZY` (mặc định)
- `@OneToMany`: `FetchType.LAZY`
- Chỉ dùng `EAGER` khi thực sự cần thiết

## 5. INDEXES
- Index cho các trường thường xuyên tìm kiếm: `orderNumber`, `username`, `email`, `tableNumber`
- Index cho các trường date: `createdAt`, `reservationDate`, `shiftDate`

## 6. NAMING CONVENTION
- **Table names**: `snake_case` (users, orders, order_items)
- **Column names**: `snake_case` (user_id, order_number, created_at)
- **Entity names**: `PascalCase` (User, Order, OrderItem)

## 7. AUDIT FIELDS
- `createdAt`: Tự động set khi tạo
- `updatedAt`: Tự động update khi sửa
- Có thể dùng `@EntityListeners` và `@PrePersist`, `@PreUpdate`

## 8. SOFT DELETE (Tùy chọn)
- Thêm trường `deleted`: Boolean (Default: false)
- Không xóa thực sự, chỉ đánh dấu đã xóa
