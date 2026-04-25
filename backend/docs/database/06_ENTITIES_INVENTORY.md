# Entities - Inventory Management

## INVENTORY (Kho hàng - Tùy chọn nâng cao)

### Trường
- `id`: Long (Primary Key, Auto Increment)
- `ingredientName`: String (Unique, Not Null)
- `quantity`: Double (Not Null, Min: 0)
- `unit`: String (Not Null, ví dụ: "kg", "lít", "cái")
- `minStockLevel`: Double (Not Null, Min: 0)
- `status`: InventoryStatus (Not Null, Enum, tính toán tự động)
- `lastUpdated`: LocalDateTime (Not Null)

### Relationships
- Không có relationship với entities khác (có thể mở rộng sau)

### Indexes
- `ingredientName` (Unique)

### Lưu ý
- Module này là tùy chọn, có thể thêm vào sau
- `status` được tính toán tự động dựa trên `quantity` và `minStockLevel`
