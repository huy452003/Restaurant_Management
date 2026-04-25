# Entities - Menu Management Domain

## 1. CATEGORY (Danh mục món ăn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `name`: String (Unique, Not Null)
- `description`: String
- `imageUrl`: String
- `status`: MenuItemStatus (Not Null, Enum, Default: ACTIVE)
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `OneToMany` → MenuItem

### Indexes
- `name` (Unique)

---

## 2. MENU_ITEM (Món ăn)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `name`: String (Not Null)
- `description`: String
- `price`: BigDecimal (Not Null, Min: 0)
- `imageUrl`: String
- `category`: Category (ManyToOne, Not Null)
- `status`: MenuItemStatus (Not Null, Enum, Default: AVAILABLE)
- `preparationTime`: Integer (phút, Min: 0)
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `ManyToOne` → Category
- `OneToMany` → OrderItem
- `ManyToMany` → Promotion (applicableItems)

### Indexes
- `category_id` (Foreign Key)

### Lưu ý
- `OUT_OF_STOCK`: Không cho phép thêm vào Order
- `DISCONTINUED`: Ẩn khỏi menu, nhưng vẫn hiển thị trong Order cũ

---

## 3. PROMOTION (Khuyến mãi)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `name`: String (Not Null)
- `description`: String
- `discountType`: DiscountType (Not Null, Enum)
- `discountValue`: BigDecimal (Not Null, Min: 0)
- `minOrderAmount`: BigDecimal (Min: 0, optional)
- `startDate`: LocalDateTime (Not Null)
- `endDate`: LocalDateTime (Not Null)
- `status`: PromotionStatus (Not Null, Enum, Default: ACTIVE)
- `createdAt`: LocalDateTime (Not Null)
- `updatedAt`: LocalDateTime

### Relationships
- `ManyToMany` → MenuItem (applicableItems) - Optional, nếu null thì áp dụng cho tất cả

### Indexes
- `startDate`, `endDate` (Index cho tìm kiếm theo thời gian)
