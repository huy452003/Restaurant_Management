# Backend Next Steps Roadmap

## 1) Thu tu lam viec

1. `Category`
2. `Table`
3. `Reservation`
4. `Order`
5. `OrderItem`
6. `Payment`
7. `Inventory`
8. `Shift`

## 2) Rule chung

- Chi dung read API: `filters(..., Pageable pageable)`
- Khong hard delete
- `User` giu status tach rieng
- Cac module con lai: cap nhat status trong `update` (khong tach API status rieng)
- Pattern code: `Controller -> Service -> Repository`

## 3) Checklist theo module

### 3.1 Category

- Controller
  - `Page<CategoryModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<CategoryModel> create(List<CategoryModel> categories)`
  - `List<CategoryModel> update(List<CategoryModel> updates, List<Integer> categoryIds)`
- Service
  - `Page<CategoryModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<CategoryModel> create(List<CategoryModel> categories)`
  - `List<CategoryModel> update(List<CategoryModel> updates, List<Integer> categoryIds)`
- Repository
  - `boolean existsByName(String name)`
  - `List<CategoryEntity> findByNameIn(List<String> names)`
  - `List<CategoryEntity> findAllByIdIn(List<Integer> ids)`

### 3.2 Table

- Controller
  - `Page<TableModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<TableModel> create(List<TableModel> tables)`
  - `List<TableModel> update(List<TableModel> updates, List<Integer> tableIds)`
- Service
  - `Page<TableModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<TableModel> create(List<TableModel> tables)`
  - `List<TableModel> update(List<TableModel> updates, List<Integer> tableIds)`
- Repository
  - `boolean existsByTableNumber(Integer tableNumber)`

### 3.3 Reservation

- Controller
  - `Page<ReservationModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<ReservationModel> create(List<ReservationModel> reservations)`
  - `List<ReservationModel> update(List<ReservationModel> updates, List<Integer> reservationIds)`
- Service
  - `Page<ReservationModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<ReservationModel> create(List<ReservationModel> reservations)`
  - `List<ReservationModel> update(List<ReservationModel> updates, List<Integer> reservationIds)`
  - `boolean hasTimeConflict(Integer tableId, LocalDateTime reservationTs, Integer excludeReservationId)`
- Repository
  - `boolean existsConflict(Integer tableId, LocalDateTime start, LocalDateTime end, Integer excludeId)`

### 3.4 Order

- Controller
  - `Page<OrderModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `OrderModel create(CreateOrderRequest request)`
  - `OrderModel update(OrderModel update, Integer orderId)`
  - `OrderSummaryModel recalculate(Integer orderId)`
- Service
  - `Page<OrderModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `OrderModel create(CreateOrderRequest request)`
  - `OrderModel update(OrderModel update, Integer orderId)`
  - `OrderSummaryModel recalculate(Integer orderId)`
  - `void syncTableStatusWhenOrderChanged(OrderEntity order)`
- Repository
  - `boolean existsByOrderNumber(String orderNumber)`

### 3.5 OrderItem

- Controller
  - `Page<OrderItemModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<OrderItemModel> addItems(Integer orderId, List<AddOrderItemRequest> items)`
  - `List<OrderItemModel> updateItems(List<OrderItemModel> updates, List<Integer> orderItemIds)`
- Service
  - `Page<OrderItemModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<OrderItemModel> addItems(Integer orderId, List<AddOrderItemRequest> items)`
  - `List<OrderItemModel> updateItems(List<OrderItemModel> updates, List<Integer> orderItemIds)`
  - `BigDecimal calculateSubTotal(Integer quantity, BigDecimal unitPrice)`
  - `void recalculateOrderTotals(Integer orderId)`
- Repository
  - `List<OrderItemEntity> findAllByIdIn(List<Integer> ids)`
  - `List<OrderItemEntity> findByOrderIdAndOrderItemStatus(Integer orderId, OrderItemStatus status)`

### 3.6 Payment

- Controller
  - `Page<PaymentModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `PaymentModel create(PaymentModel payment)`
  - `PaymentModel update(PaymentModel update, Integer paymentId)`
  - `PaymentSummaryModel getOrderPaymentSummary(Integer orderId)`
- Service
  - `Page<PaymentModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `PaymentModel create(PaymentModel payment)`
  - `PaymentModel update(PaymentModel update, Integer paymentId)`
  - `PaymentSummaryModel getOrderPaymentSummary(Integer orderId)`
  - `boolean isOrderFullyPaid(Integer orderId)`
  - `void syncOrderStatusAfterPayment(Integer orderId)`
- Repository
  - `BigDecimal sumSuccessAmountByOrderId(Integer orderId)`

### 3.7 Inventory

- Controller
  - `Page<InventoryModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<InventoryModel> create(List<InventoryModel> items)`
  - `List<InventoryModel> update(List<InventoryModel> updates, List<Integer> inventoryIds)`
  - `InventoryModel restock(Integer inventoryId, Integer quantityAdded)`
- Service
  - `Page<InventoryModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<InventoryModel> create(List<InventoryModel> items)`
  - `List<InventoryModel> update(List<InventoryModel> updates, List<Integer> inventoryIds)`
  - `InventoryModel restock(Integer inventoryId, Integer quantityAdded)`
  - `InventoryStatus evaluateStatus(Integer quantity, Integer minStockLevel)`
  - `void consumeIngredient(Integer inventoryId, Integer quantityUsed)`
- Repository
  - `boolean existsByIngredientName(String ingredientName)`

### 3.8 Shift

- Controller
  - `Page<ShiftModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<ShiftModel> create(List<ShiftModel> shifts)`
  - `List<ShiftModel> update(List<ShiftModel> updates, List<Integer> shiftIds)`
- Service
  - `Page<ShiftModel> filters(Map<String, Object> filters, Pageable pageable)`
  - `List<ShiftModel> create(List<ShiftModel> shifts)`
  - `List<ShiftModel> update(List<ShiftModel> updates, List<Integer> shiftIds)`
  - `boolean hasOverlap(Integer employeeId, LocalDateTime startTime, LocalDateTime endTime, Integer excludeShiftId)`
- Repository
  - `boolean existsOverlap(Integer employeeId, LocalDateTime startTime, LocalDateTime endTime, Integer excludeId)`
