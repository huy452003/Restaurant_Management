# Entities - Order Management Domain

## 1. ORDER (Đơn hàng)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `orderNumber`: String (Unique, Not Null, Auto-generated)
- `table`: Table (ManyToOne, nullable cho TAKEAWAY/DELIVERY)
- `waiter`: User (ManyToOne, Not Null)
- `status`: OrderStatus (Not Null, Enum, Default: PENDING)
- `orderType`: OrderType (Not Null, Enum)
- `subTotal`: BigDecimal (Not Null, Min: 0)
- `tax`: BigDecimal (Not Null, Min: 0, Default: 0)
- `discount`: BigDecimal (Not Null, Min: 0, Default: 0)
- `totalAmount`: BigDecimal (Not Null, Min: 0)
- `notes`: String
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime
- `completedAt`: LocalDateTime

### Relationships
- `ManyToOne` → Table
- `ManyToOne` → User (waiter)
- `OneToMany` → OrderItem
- `OneToMany` → Payment

### Indexes
- `orderNumber` (Unique)
- `table_id` (Foreign Key)
- `waiter_id` (Foreign Key)
- `createdAt` (Index cho tìm kiếm theo ngày)

### Lưu ý
- `orderNumber`: Format "ORD-YYYYMMDD-XXXX" (ví dụ: ORD-20241222-0001)
- `totalAmount`: Tính toán = subTotal + tax - discount

---

## 2. ORDER_ITEM (Chi tiết món trong đơn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `order`: Order (ManyToOne, Not Null)
- `menuItem`: MenuItem (ManyToOne, Not Null)
- `quantity`: Integer (Not Null, Min: 1)
- `unitPrice`: BigDecimal (Not Null, Min: 0) - Lưu giá tại thời điểm đặt
- `subTotal`: BigDecimal (Not Null, Min: 0) - quantity * unitPrice
- `specialInstructions`: String
- `status`: OrderItemStatus (Not Null, Enum, Default: PENDING)

### Relationships
- `ManyToOne` → Order
- `ManyToOne` → MenuItem

### Indexes
- `order_id` (Foreign Key)
- `menu_item_id` (Foreign Key)

### Lưu ý
- `unitPrice`: Lưu giá tại thời điểm đặt hàng, không thay đổi dù MenuItem.price thay đổi sau đó

---

## 3. PAYMENT (Thanh toán)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `order`: Order (ManyToOne, Not Null)
- `cashier`: User (ManyToOne, Not Null)
- `paymentMethod`: PaymentMethod (Not Null, Enum)
- `amount`: BigDecimal (Not Null, Min: 0)
- `status`: PaymentStatus (Not Null, Enum, Default: PENDING)
- `transactionId`: String (Unique, optional - cho thanh toán online)
- `paidAt`: LocalDateTime
- `createdAt`: LocalDateTime (Not Null)

### Relationships
- `ManyToOne` → Order
- `ManyToOne` → User (cashier)

### Indexes
- `order_id` (Foreign Key)
- `cashier_id` (Foreign Key)
- `transactionId` (Unique, nullable)

### Lưu ý
- Có thể thanh toán nhiều lần cho 1 Order
- Tổng Payment.amount có thể <= Order.totalAmount

---

## 4. REVIEW (Đánh giá - Tùy chọn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `order`: Order (ManyToOne, Not Null)
- `rating`: Integer (Not Null, Min: 1, Max: 5)
- `comment`: String
- `customerName`: String (Not Null)
- `createdAt`: LocalDateTime (Not Null)

### Relationships
- `ManyToOne` → Order

### Indexes
- `order_id` (Foreign Key)
- `rating` (Index cho tìm kiếm theo điểm)
