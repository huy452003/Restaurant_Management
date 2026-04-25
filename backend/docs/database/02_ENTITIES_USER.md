# Entities - User Management Domain

## 1. USER (Người dùng)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `username`: String (Unique, Not Null)
- `email`: String (Unique, Not Null)
- `password`: String (Not Null, Encrypted)
- `fullName`: String (Not Null)
- `phone`: String
- `address`: String
- `role`: UserRole (Not Null, Enum)
- `status`: UserStatus (Not Null, Enum, Default: ACTIVE)
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `OneToMany` → Order (waiter)
- `OneToMany` → Payment (cashier)
- `OneToMany` → Shift (employee)

### Indexes
- `username` (Unique)
- `email` (Unique)

---

## 2. SHIFT (Ca làm việc)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `employee`: User (ManyToOne, Not Null)
- `shiftDate`: LocalDate (Not Null)
- `startTime`: LocalTime (Not Null)
- `endTime`: LocalTime (Not Null)
- `status`: ShiftStatus (Not Null, Enum, Default: SCHEDULED)
- `notes`: String
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `ManyToOne` → User (employee)

### Indexes
- `employee_id` (Foreign Key)
- `shiftDate` (Index cho tìm kiếm theo ngày)
