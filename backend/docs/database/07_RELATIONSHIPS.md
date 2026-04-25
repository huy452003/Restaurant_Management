# Sơ Đồ Quan Hệ (Relationships)

## User Management
```
USER (1) ──→ (N) ORDER (waiter)
USER (1) ──→ (N) PAYMENT (cashier)
USER (1) ──→ (N) SHIFT (employee)
```

## Menu Management
```
CATEGORY (1) ──→ (N) MENU_ITEM

PROMOTION (N) ──→ (N) MENU_ITEM (ManyToMany - applicableItems)
```

## Order Management
```
ORDER (1) ──→ (N) ORDER_ITEM
ORDER (1) ──→ (N) PAYMENT
ORDER (1) ──→ (N) REVIEW

MENU_ITEM (1) ──→ (N) ORDER_ITEM
```

## Table Management
```
TABLE (1) ──→ (N) ORDER
TABLE (1) ──→ (N) RESERVATION
```

## Tóm tắt
- **User** có quan hệ với: Order (waiter), Payment (cashier), Shift (employee)
- **Category** có quan hệ với: MenuItem (OneToMany)
- **MenuItem** có quan hệ với: Category, OrderItem, Promotion
- **Order** có quan hệ với: Table, User (waiter), OrderItem, Payment, Review
- **Table** có quan hệ với: Order, Reservation
