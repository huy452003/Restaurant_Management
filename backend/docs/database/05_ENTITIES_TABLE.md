# Entities - Table Management Domain

## 1. TABLE (Bàn ăn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `tableNumber`: String (Unique, Not Null, ví dụ: "T01", "T02")
- `capacity`: Integer (Not Null, Min: 1)
- `status`: TableStatus (Not Null, Enum, Default: AVAILABLE)
- `location`: String (ví dụ: "Tầng 1", "Khu vực A")
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `OneToMany` → Order
- `OneToMany` → Reservation

### Indexes
- `tableNumber` (Unique)

### Lưu ý
- `status` tự động update:
  - `OCCUPIED` khi có Order mới
  - `AVAILABLE` khi Order hoàn thành

---

## 2. RESERVATION (Đặt bàn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `customerName`: String (Not Null)
- `customerPhone`: String (Not Null)
- `customerEmail`: String
- `table`: Table (ManyToOne, Not Null)
- `reservationDate`: LocalDate (Not Null)
- `reservationTime`: LocalTime (Not Null)
- `numberOfGuests`: Integer (Not Null, Min: 1)
- `status`: ReservationStatus (Not Null, Enum, Default: PENDING)
- `specialRequests`: String
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `ManyToOne` → Table

### Indexes
- `table_id` (Foreign Key)
- `reservationDate` (Index cho tìm kiếm theo ngày)
- `customerPhone` (Index)
