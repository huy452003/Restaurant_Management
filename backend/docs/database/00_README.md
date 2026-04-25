# Database Design - Hệ Thống Quản Lý Nhà Hàng

## Mục lục

1. **[01_ENUMS.md](01_ENUMS.md)** - Danh sách tất cả Enums
2. **[02_ENTITIES_USER.md](02_ENTITIES_USER.md)** - User Management Domain (User, Shift)
3. **[03_ENTITIES_MENU.md](03_ENTITIES_MENU.md)** - Menu Management Domain (Category, MenuItem, Promotion)
4. **[04_ENTITIES_ORDER.md](04_ENTITIES_ORDER.md)** - Order Management Domain (Order, OrderItem, Payment, Review)
5. **[05_ENTITIES_TABLE.md](05_ENTITIES_TABLE.md)** - Table Management Domain (Table, Reservation)
6. **[06_ENTITIES_INVENTORY.md](06_ENTITIES_INVENTORY.md)** - Inventory Management (Inventory)
7. **[07_RELATIONSHIPS.md](07_RELATIONSHIPS.md)** - Sơ đồ quan hệ giữa các entities
8. **[08_DESIGN_GUIDELINES.md](08_DESIGN_GUIDELINES.md)** - Best practices và guidelines
9. **[09_IMPORTANT_NOTES.md](09_IMPORTANT_NOTES.md)** - Các trường quan trọng cần lưu ý

## Tổng quan

Database được thiết kế theo **domain-driven design** với các domain chính:

### 📦 Các Domains (tổ chức theo packages trong code, không phải modules riêng)
- **User Domain**: Quản lý người dùng và ca làm việc (User, Shift)
- **Menu Domain**: Quản lý danh mục, món ăn và khuyến mãi (Category, MenuItem, Promotion)
- **Order Domain**: Quản lý đơn hàng, thanh toán và đánh giá (Order, OrderItem, Payment, Review)
- **Table Domain**: Quản lý bàn ăn và đặt bàn (Table, Reservation)
- **Inventory Domain**: Quản lý kho hàng (Inventory) - Tùy chọn

### 🏗️ Kiến trúc Multi-Module
- **`common`** (JAR): Chứa models, enums, config, i18n dùng chung
- **Application modules** (JAR executable): Các module ứng dụng chạy độc lập
  - Có thể có 1 hoặc nhiều application modules tùy theo kiến trúc
  - Chỉ module **application** là có `@SpringBootApplication` và có thể chạy

> **Lưu ý**: "Management" ở đây chỉ là cách tổ chức **domain/packages** trong code, **KHÔNG** phải modules riêng biệt!
