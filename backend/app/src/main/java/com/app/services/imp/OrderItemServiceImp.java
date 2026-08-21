package com.app.services.imp;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.app.services.OrderItemService;
import com.app.services.PaymentService;
import com.app.services.TableStatusSyncService;
import com.app.utils.OrderStatusTransitionUtils;
import com.app.utils.UserEntityUtils;
import com.common.repositories.MenuItemRepository;
import com.common.repositories.OrderItemRepository;
import com.common.repositories.OrderRepository;
import com.common.repositories.PaymentRepository;
import com.common.enums.PaymentStatus;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.models.orderItem.OrderItemAdminUpdateModel;
import com.common.models.orderItem.OrderItemCreateModel;
import com.common.models.orderItem.OrderItemCustomerUpdateModel;
import com.common.models.orderItem.OrderItemModel;
import com.common.entities.MenuItemEntity;
import com.common.entities.OrderEntity;
import com.common.entities.OrderItemEntity;
import com.common.entities.UserEntity;
import com.common.enums.MenuItemStatus;
import com.common.enums.OrderStatus;
import com.common.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.modelmapper.ModelMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImp implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserEntityUtils userEntityUtils;
    private final MenuItemRepository menuItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoggingService log;
    private final ModelMapper modelMapper;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final TableStatusSyncService tableStatusSyncService;

    private LogContext getLogContext(String methodName, List<Integer> orderItemIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(orderItemIds)
            .build();
    }

    private static final String ORDER_ITEM_REDIS_KEY_PREFIX = "order-item:";
    private static final String ORDER_REDIS_KEY_PREFIX = "order:";
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    @Override
    @CircuitBreaker(name = "order-item-service-read", fallbackMethod = "filtersFallback")
    public Page<OrderItemModel> filters(
        Integer id, String orderNumber, OrderStatus orderItemStatus, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering order items with pagination ...!", logContext);

        List<FilterCondition<OrderItemEntity>> conditions = buildFilterConditions(
            id, orderNumber, orderItemStatus
        );
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext
        );
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            conditions.add(FilterCondition.eq("order.customerEmail", currentUser.getEmail()));
        }

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            ORDER_ITEM_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<OrderItemModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, OrderItemModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " order items in cache", logContext);
            return cached;
        }

        Page<OrderItemEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = orderItemRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all order items with pagination", logContext);
        }else {
            Specification<OrderItemEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = orderItemRepository.findAll(spec, pageable);
        }

        List<OrderItemModel> pageDatas = pageEntities.getContent().stream().map(
            this::toOrderItemModel
        ).collect(Collectors.toList());

        Page<OrderItemModel> orderItemModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, orderItemModelPage);
            log.logInfo("cached first-page filter snapshot for " + orderItemModelPage.getTotalElements()
                + " order items, key: " + redisKeyFilters, logContext);
        }
        return orderItemModelPage;
    }

    @Override
    @CircuitBreaker(name = "order-item-service-write", fallbackMethod = "createsFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public List<OrderItemModel> create(List<OrderItemCreateModel> orderItems) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating order items ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext
        );

        Map<String, OrderEntity> ordersByNumber = orderItems.stream()
            .map(OrderItemCreateModel::getOrderNumber)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(), 
                orderNumber -> resolveOrder(orderNumber, logContext)
            )
        );

        Map<String, MenuItemEntity> menuItemsByName = orderItems.stream()
            .map(OrderItemCreateModel::getMenuItemName)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(), 
                menuItemName -> resolveMenuItem(menuItemName, logContext)
            )
        );

        for(OrderEntity order : ordersByNumber.values()) {
            // kiểm tra xem user có quyền thao tác với order items của order đó không
            assertCanMutateOrderItems(order, currentUser, logContext);
        }

        List<Integer> orderIds = ordersByNumber.values().stream()
            .map(OrderEntity::getId)
            .distinct()
            .collect(Collectors.toList());

        // tạo map để gộp dòng cùng đơn + cùng món + cùng ghi chú bếp (specialInstructions) thành 1 key duy nhất
        Map<String, OrderItemEntity> activeLinesByMergeKey = new HashMap<>();

        for (OrderItemEntity existing : orderItemRepository.findByOrder_IdIn(orderIds)) {
            if (existing.getOrderItemStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            if (existing.getOrder() == null || existing.getMenuItem() == null) {
                continue;
            }
            activeLinesByMergeKey.putIfAbsent(
                mergeKeyForOrderLine(
                    existing.getOrder().getId(),
                    existing.getMenuItem().getId(),
                    existing.getSpecialInstructions()
                ),
                existing
            );
        }

        List<OrderItemEntity> toInsert = new ArrayList<>();
        List<OrderItemEntity> toMerge = new ArrayList<>();
        for (OrderItemCreateModel orderItemModel : orderItems) {
            OrderEntity order = ordersByNumber.get(orderItemModel.getOrderNumber());
            MenuItemEntity menuItem = menuItemsByName.get(orderItemModel.getMenuItemName());
            String mergeKey = mergeKeyForOrderLine(
                order.getId(),
                menuItem.getId(),
                orderItemModel.getSpecialInstructions()
            );

            OrderItemEntity existing = activeLinesByMergeKey.get(mergeKey);
            // nếu đã có dòng order item tồn tại thì gộp dòng đó với dòng mới
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + orderItemModel.getQuantity());
                applyPricingFromMenu(existing, menuItem);
                if (!toMerge.contains(existing)) {
                    toMerge.add(existing);
                }
                continue;
            }
            // nếu không có dòng order item tồn tại thì tạo dòng mới
            OrderItemEntity entity = modelMapper.map(orderItemModel, OrderItemEntity.class);
            entity.setOrder(order);
            entity.setMenuItem(menuItem);
            entity.setOrderItemStatus(OrderStatus.PENDING);
            applyPricingFromMenu(entity, menuItem);
            toInsert.add(entity);
            activeLinesByMergeKey.put(mergeKey, entity);
            toMerge.add(entity);
        }

        if (!toInsert.isEmpty()) {
            orderItemRepository.saveAll(toInsert);
        }

        List<OrderItemEntity> updated = toMerge.stream()
            .filter(line -> line.getId() != null)
            .collect(Collectors.toList());

        if (!updated.isEmpty()) {
            orderItemRepository.saveAll(updated);
        }

        recalculateOrdersFromItems(orderIds);

        clearOrderCaches(logContext);

        log.logInfo("Deleted filter caches after create", logContext);

        log.logInfo(
            "completed, " + toInsert.size() + " new line(s), "
                + (toMerge.size() - toInsert.size()) + " merged into existing line(s)",
            logContext
        );
        return toMerge.stream().map(this::toOrderItemModel).collect(Collectors.toList());
    }

    @Override
    @CircuitBreaker(name = "order-item-service-write", fallbackMethod = "updateForCustomerFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public OrderItemModel updateForCustomer(OrderItemCustomerUpdateModel update, Integer orderItemId) {
        LogContext logContext = getLogContext("updateForCustomer", Collections.singletonList(orderItemId));
        log.logInfo("Updating order item by customer ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext
        );
        OrderItemEntity current = orderItemRepository.findById(orderItemId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order item not found with id: " + orderItemId,
                Collections.singletonList(orderItemId),
                "OrderItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

        assertCanMutateOrderItems(current.getOrder(), currentUser, logContext);

        boolean hasChanges = !Objects.equals(update.getQuantity(), current.getQuantity()) || 
                             !Objects.equals(update.getSpecialInstructions(), current.getSpecialInstructions());

        if(hasChanges) {
            current.setQuantity(update.getQuantity());
            current.setSpecialInstructions(update.getSpecialInstructions());
            applyPricingFromMenu(current, current.getMenuItem());
            orderItemRepository.saveAndFlush(current);
            recalculateOrdersFromItems(Collections.singletonList(current.getOrder().getId()));
            clearOrderCaches(logContext);
            log.logInfo("completed, updated order item with id: " + orderItemId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        return toOrderItemModel(current);
    }

    @Override
    @CircuitBreaker(name = "order-item-service-admin-write", fallbackMethod = "updateByAdminFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<OrderItemModel> updateByAdmin(List<OrderItemAdminUpdateModel> updates, List<Integer> orderItemIds) {
        LogContext logContext = getLogContext("updateByAdmin", orderItemIds);
        log.logInfo("Updating order items by admin/manager ...!", logContext);

        if(updates.size() != orderItemIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and orderItemIds",
                orderItemIds,
                "OrderItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext
        );

        // query với map để tránh N+1 query
        Map<Integer, OrderItemEntity> orderItemsById = orderItemRepository
            .findAllById(orderItemIds)
            .stream()
            .collect(Collectors.toMap(OrderItemEntity::getId, Function.identity()));

        // batch lại đúng thứ tự với list ids từ request
        List<OrderItemEntity> foundOrderItems = orderItemIds.stream().map(id -> {
            OrderItemEntity orderItem = orderItemsById.get(id);
            if (orderItem != null) {
                return orderItem;
            }
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order item not found with id: " + id,
                Collections.singletonList(id),
                "OrderItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }).collect(Collectors.toList());
        log.logInfo("found " + foundOrderItems.size() + " order items", logContext);

        for(OrderItemEntity orderItem : foundOrderItems) {
            assertCanMutateOrderItems(orderItem.getOrder(), currentUser, logContext);
        }

        List<OrderItemEntity> orderItemsToUpdate = new ArrayList<>();
        Iterator<OrderItemAdminUpdateModel> orderItemIterator = updates.iterator();
        Iterator<OrderItemEntity> currentOrderItemIterator = foundOrderItems.iterator();

        while(orderItemIterator.hasNext() && currentOrderItemIterator.hasNext()) {
            OrderItemAdminUpdateModel update = orderItemIterator.next();
            OrderItemEntity current = currentOrderItemIterator.next();

            Boolean hasChanges = !Objects.equals(update.getMenuItemName(), current.getMenuItem().getName()) ||
                                 !Objects.equals(update.getQuantity(), current.getQuantity()) ||
                                 !Objects.equals(update.getSpecialInstructions(), current.getSpecialInstructions()) ||
                                 !Objects.equals(update.getOrderItemStatus(), current.getOrderItemStatus());
            if(hasChanges) {
                modelMapper.map(update, current);
                MenuItemEntity menuItem = resolveMenuItem(update.getMenuItemName(), logContext);
                current.setMenuItem(menuItem);
                applyPricingFromMenu(current, menuItem);
                orderItemsToUpdate.add(current);
            }
        }

        if(!orderItemsToUpdate.isEmpty()) {
            orderItemRepository.saveAllAndFlush(orderItemsToUpdate);
            List<Integer> affectedOrderIds = orderItemsToUpdate.stream()
                .map(orderItem -> orderItem.getOrder().getId())
                .distinct()
                .collect(Collectors.toList());
            recalculateOrdersFromItems(affectedOrderIds);
            log.logInfo("completed, updated " + orderItemsToUpdate.size() + " order items", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        clearOrderCaches(logContext);

        return foundOrderItems.stream().map(
            this::toOrderItemModel
        ).collect(Collectors.toList());
    }

    // ======================================== Helper Methods ========================================

    private OrderItemModel toOrderItemModel(OrderItemEntity entity) {
        OrderItemModel orderItemModel = modelMapper.map(entity, OrderItemModel.class);
        if(entity.getOrder() != null) {
            orderItemModel.setOrderNumber(entity.getOrder().getOrderNumber());
        }
        if(entity.getMenuItem() != null) {
            orderItemModel.setMenuItemName(entity.getMenuItem().getName());
        }
        return orderItemModel;
    }

    // kiểm tra xem user có quyền thao tác với order items của order đó không
    private void assertCanMutateOrderItems(
        OrderEntity order, UserEntity currentUser, LogContext logContext
    ) {
        // nếu là customer
        if(currentUser.getRole() == UserRole.CUSTOMER) {
            if(order.getOrderStatus() != OrderStatus.PENDING) {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "You can only add or change items while the order is pending",
                    "OrderItemModel",
                    "order status must be PENDING"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
            if(!Objects.equals(order.getCustomerEmail(), currentUser.getEmail())) {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "You can only manage items on your own orders",
                    "OrderItemModel",
                    "order owner must match authenticated customer"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
        } else {
            // nếu là admin/manager
            // chỉ cho phép update items khi order chưa được cancel/complete
            OrderStatus status = order.getOrderStatus();
            if(status == OrderStatus.CANCELLED || status == OrderStatus.COMPLETED) {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "Cannot change items when order is cancelled or completed",
                    "OrderItemModel",
                    "order must not be CANCELLED or COMPLETED"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
        }
    }

    private void clearOrderCaches(LogContext logContext) {
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_ITEM_REDIS_KEY_PREFIX);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("Deleted order-item and order filter caches after mutation", logContext);
    }

    // tính toán lại tổng amount của order từ order items và cập nhật lại trạng thái của order
    private void recalculateOrdersFromItems(List<Integer> orderIds) {
        if(orderIds == null || orderIds.isEmpty()) {
            return;
        }
        List<OrderEntity> orders = orderRepository.findAllById(orderIds);

        Map<Integer, List<OrderItemEntity>> itemsByOrderId =
            // lấy tất cả order items theo order ids
            orderItemRepository.findByOrder_IdIn(orderIds)
            .stream()
            // ở đây ta khai báo gom lại theo item.getOrder().getId() ( key của map )
            // value của map là list của order items (đã query phía trên), java stream sẽ tự động gom lại theo key
            // nếu chưa có key thì sẽ tạo mới key đó, new list rồi add value vào list
            // nếu đã có key thì sẽ add value vào list của key đó
            .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        Set<Integer> tablesToRecheck = new HashSet<>();

        for(OrderEntity order : orders) {

            List<OrderItemEntity> orderItems = itemsByOrderId.getOrDefault(
                order.getId(), Collections.emptyList()
            );

            BigDecimal subTotal = orderItems.stream()
                .map(OrderItemEntity::getSubTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tax = subTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            // cập nhật lại tổng amount của order
            order.setSubTotal(subTotal);
            order.setTax(tax);
            order.setTotalAmount(totalAmount);

            List<OrderStatus> itemStatuses = orderItems.stream()
                .map(OrderItemEntity::getOrderItemStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if(itemStatuses.isEmpty()) {
                continue;
            }

            // xác định trạng thái mới của order từ trạng thái của order items
            OrderStatus targetStatus = determineOrderStatusFromItems(itemStatuses);
            // lấy trạng thái hiện tại của order
            OrderStatus previousOrderStatus = order.getOrderStatus();

            // kiểm tra xem đã có payment completed nào của order này chưa
            boolean paid = paymentRepository.existsByOrderIdAndPaymentStatus(
                order.getId(), PaymentStatus.COMPLETED
            );

            // kiểm tra xem trạng thái của order có được phép chuyển sang trạng thái mới không
            // nếu không được phép thì skip
            if (!OrderStatusTransitionUtils.isAllowedTransition(
                previousOrderStatus, targetStatus, paid
            )) {
                continue;
            }
            // áp dụng trạng thái mới của order
            OrderStatusTransitionUtils.applyOrderStatusTransition(order, targetStatus, paid);
            // nếu order muốn chuyển sang cancelled thì hủy payment pending của order đó
            if (targetStatus == OrderStatus.CANCELLED && previousOrderStatus != OrderStatus.CANCELLED) {
                paymentService.cancelPendingPaymentsForOrder(order.getId());
            }
            // Lần đầu chuyển sang COMPLETED hoặc CANCELLED → sync trạng thái bàn sau save
            boolean becameTerminal = (targetStatus == OrderStatus.CANCELLED || targetStatus == OrderStatus.COMPLETED)
                && previousOrderStatus != targetStatus;
            // thêm bàn vào set để sync lại status của bàn
            if (becameTerminal && order.getTable() != null) {
                tablesToRecheck.add(order.getTable().getTableNumber());
            }
        }
        // cập nhật lại order
        orderRepository.saveAllAndFlush(orders);
        // sync lại status của bàn
        tablesToRecheck.forEach(tableStatusSyncService::syncTableStatus);
    }

    // xác định trạng thái của order từ trạng thái của order items
    private OrderStatus determineOrderStatusFromItems(List<OrderStatus> itemStatuses) {
        // nếu tất cả order items đều là CANCELLED thì order status là CANCELLED
        if(itemStatuses.stream().allMatch(status -> status == OrderStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        // nếu tất cả order items đều là COMPLETED thì order status là COMPLETED
        if(itemStatuses.stream().allMatch(status -> status == OrderStatus.COMPLETED)) {
            return OrderStatus.COMPLETED;
        }
        // nếu có ít nhất 1 order item là PREPARING thì order status là PREPARING
        if(itemStatuses.stream().anyMatch(status -> status == OrderStatus.PREPARING)) {
            return OrderStatus.PREPARING;
        }
        // nếu có ít nhất 1 order item là CONFIRMED thì order status là CONFIRMED
        if(itemStatuses.stream().anyMatch(status -> status == OrderStatus.CONFIRMED)) {
            return OrderStatus.CONFIRMED;
        }
        return OrderStatus.PENDING;
    }

    // gộp dòng cùng đơn + cùng món + cùng ghi chú bếp (specialInstructions) thành 1 key duy nhất
    private static String mergeKeyForOrderLine(
        Integer orderId, Integer menuItemId, String specialInstructions
    ) {
        return orderId + ":" + menuItemId + ":" + normalizeSpecialInstructions(specialInstructions);
    }

    private static String normalizeSpecialInstructions(String specialInstructions) {
        if (!StringUtils.hasText(specialInstructions)) {
            return "";
        }
        return specialInstructions.trim();
    }

    // tính toán lại unit price và sub total của order item từ menu item
    private void applyPricingFromMenu(OrderItemEntity orderItem, MenuItemEntity menuItem) {
        BigDecimal unitPrice = menuItem.getPrice();
        BigDecimal subTotal = unitPrice
            .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setSubTotal(subTotal);
    }

    private OrderEntity resolveOrder(String orderNumber, LogContext logContext) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with orderNumber: " + orderNumber,
                Collections.singletonList(orderNumber),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private MenuItemEntity resolveMenuItem(String menuItemName, LogContext logContext) {
        MenuItemEntity menuItem = menuItemRepository.findByName(menuItemName).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "MenuItem not found with name: " + menuItemName,
                Collections.singletonList(menuItemName),
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
        if (menuItem.getMenuItemStatus() != MenuItemStatus.AVAILABLE) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Menu item is not available: " + menuItemName,
                Collections.singletonList(menuItemName),
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        return menuItem;
    }

    private List<FilterCondition<OrderItemEntity>> buildFilterConditions(
        Integer id, String orderNumber, OrderStatus orderItemStatus
    ) {
        List<FilterCondition<OrderItemEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(StringUtils.hasText(orderNumber)) {
            conditions.add(FilterCondition.eq("order.orderNumber", orderNumber));
        }
        if(orderItemStatus != null) {
            conditions.add(FilterCondition.eq("orderItemStatus", orderItemStatus));
        }
        return conditions;
    }

    // ======================================== Fallback Methods ========================================

    @SuppressWarnings("unused")
    private Page<OrderItemModel> filtersFallback(
        Integer id, String orderNumber, OrderStatus orderItemStatus, Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<OrderItemEntity>> conditions = buildFilterConditions(
            id, orderNumber, orderItemStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            ORDER_ITEM_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<OrderItemModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, OrderItemModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo(
                "Found cache when calling fallback filters method, returning...", 
                getLogContext("filtersFallback", Collections.emptyList())
            );
            return cachedPage;
        }

        // lỗi circuit breaker open -> throw service unavailable exception
        throw ResilienceFallbackUtils.serviceUnavailable("filters", e);
    }

    @SuppressWarnings("unused")
    private List<OrderItemModel> createsFallback(List<OrderItemCreateModel> orderItems, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "creates");
        return null;
    }

    @SuppressWarnings("unused")
    private OrderItemModel updateForCustomerFallback(OrderItemCustomerUpdateModel update, Integer orderItemId, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updateForCustomer");
        return null;
    }

    @SuppressWarnings("unused")
    private List<OrderItemModel> updateByAdminFallback(List<OrderItemAdminUpdateModel> updates, List<Integer> orderItemIds, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updateByAdmin");
        return null;
    }
}
