package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;
import jakarta.transaction.Transactional;

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

@Service
public class OrderItemServiceImp implements OrderItemService {
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserEntityUtils userEntityUtils;
    @Autowired
    private MenuItemRepository menuItemRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TableStatusSyncService tableStatusSyncService;

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
    public Page<OrderItemModel> filters(
        Integer id, String orderNumber, OrderStatus orderItemStatus, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering order items with pagination ...!", logContext);

        List<FilterCondition<OrderItemEntity>> conditions = buildFilterConditions(
            id, orderNumber, orderItemStatus
        );
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext, log
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
    @Transactional
    public List<OrderItemModel> create(List<OrderItemCreateModel> orderItems) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating order items ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext, log
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
            assertCanMutateOrderItems(order, currentUser, logContext);
        }

        List<Integer> orderIds = ordersByNumber.values().stream()
            .map(OrderEntity::getId)
            .distinct()
            .collect(Collectors.toList());

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
        List<OrderItemEntity> touched = new ArrayList<>();
        for (OrderItemCreateModel orderItemModel : orderItems) {
            OrderEntity order = ordersByNumber.get(orderItemModel.getOrderNumber());
            MenuItemEntity menuItem = menuItemsByName.get(orderItemModel.getMenuItemName());
            String mergeKey = mergeKeyForOrderLine(
                order.getId(),
                menuItem.getId(),
                orderItemModel.getSpecialInstructions()
            );

            OrderItemEntity existing = activeLinesByMergeKey.get(mergeKey);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + orderItemModel.getQuantity());
                applyPricingFromMenu(existing, menuItem);
                if (!touched.contains(existing)) {
                    touched.add(existing);
                }
                continue;
            }

            OrderItemEntity entity = modelMapper.map(orderItemModel, OrderItemEntity.class);
            entity.setOrder(order);
            entity.setMenuItem(menuItem);
            entity.setOrderItemStatus(OrderStatus.PENDING);
            applyPricingFromMenu(entity, menuItem);
            toInsert.add(entity);
            activeLinesByMergeKey.put(mergeKey, entity);
            touched.add(entity);
        }

        if (!toInsert.isEmpty()) {
            orderItemRepository.saveAll(toInsert);
        }
        List<OrderItemEntity> updated = touched.stream()
            .filter(line -> line.getId() != null)
            .collect(Collectors.toList());
        if (!updated.isEmpty()) {
            orderItemRepository.saveAll(updated);
        }

        recalculateOrderAmounts(orderIds);

        clearOrderCaches(logContext);

        log.logInfo("Deleted filter caches after create", logContext);

        log.logInfo(
            "completed, " + toInsert.size() + " new line(s), "
                + (touched.size() - toInsert.size()) + " merged into existing line(s)",
            logContext
        );
        return touched.stream().map(this::toOrderItemModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderItemModel updateForCustomer(OrderItemCustomerUpdateModel update, Integer orderItemId) {
        LogContext logContext = getLogContext("updateForCustomer", Collections.singletonList(orderItemId));
        log.logInfo("Updating order item by customer ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderItemModel", logContext, log
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

        boolean hasChanges = !Objects.equals(update.getQuantity(), current.getQuantity())
            || !Objects.equals(update.getSpecialInstructions(), current.getSpecialInstructions());

        if(hasChanges) {
            current.setQuantity(update.getQuantity());
            current.setSpecialInstructions(update.getSpecialInstructions());
            applyPricingFromMenu(current, current.getMenuItem());
            orderItemRepository.save(current);
            recalculateOrderAmounts(Collections.singletonList(current.getOrder().getId()));
            clearOrderCaches(logContext);
        }

        return toOrderItemModel(current);
    }

    @Override
    @Transactional
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
            "OrderItemModel", logContext, log
        );

        List<OrderItemEntity> fetchedOrderItems = orderItemRepository.findAllById(orderItemIds);
        Map<Integer, OrderItemEntity> orderItemsById = fetchedOrderItems.stream()
            .collect(Collectors.toMap(OrderItemEntity::getId, Function.identity()));
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
            orderItemRepository.saveAll(orderItemsToUpdate);
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

    // private method

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

    private void assertCanMutateOrderItems(
        OrderEntity order, UserEntity currentUser, LogContext logContext
    ) {
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

    private void recalculateOrderAmounts(List<Integer> orderIds) {
        if(orderIds == null || orderIds.isEmpty()) {
            return;
        }
        recalculateOrdersFromItems(orderIds);
    }

    private void recalculateOrdersFromItems(List<Integer> orderIds) {
        if(orderIds == null || orderIds.isEmpty()) {
            return;
        }
        List<OrderEntity> orders = orderRepository.findAllById(orderIds);
        Map<Integer, List<OrderItemEntity>> itemsByOrderId = orderItemRepository.findByOrder_IdIn(orderIds).stream()
            .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        Set<Integer> tablesToRecheck = new HashSet<>();

        for(OrderEntity order : orders) {
            List<OrderItemEntity> orderItems = itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList());

            BigDecimal subTotal = orderItems.stream()
                .map(OrderItemEntity::getSubTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tax = subTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);
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
            OrderStatus targetStatus = determineOrderStatusFromItems(itemStatuses);
            OrderStatus previousOrderStatus = order.getOrderStatus();
            boolean paid = paymentRepository.existsByOrderIdAndPaymentStatus(
                order.getId(), PaymentStatus.COMPLETED
            );
            if (!OrderStatusTransitionUtils.isAllowedTransition(
                previousOrderStatus, targetStatus, paid
            )) {
                continue;
            }
            OrderStatusTransitionUtils.applyOrderStatusTransition(order, targetStatus, paid);
            if (targetStatus == OrderStatus.CANCELLED && previousOrderStatus != OrderStatus.CANCELLED) {
                paymentService.cancelPendingPaymentsForOrder(order.getId());
            }
            boolean becameTerminal = (targetStatus == OrderStatus.CANCELLED || targetStatus == OrderStatus.COMPLETED)
                && previousOrderStatus != targetStatus;
            if (becameTerminal && order.getTable() != null) {
                tablesToRecheck.add(order.getTable().getTableNumber());
            }
        }
        orderRepository.saveAll(orders);
        tablesToRecheck.forEach(tableStatusSyncService::syncTableStatus);
    }

    private OrderStatus determineOrderStatusFromItems(List<OrderStatus> itemStatuses) {
        if(itemStatuses.stream().allMatch(status -> status == OrderStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        if(itemStatuses.stream().allMatch(status -> status == OrderStatus.COMPLETED)) {
            return OrderStatus.COMPLETED;
        }
        if(itemStatuses.stream().anyMatch(status -> status == OrderStatus.PREPARING)) {
            return OrderStatus.PREPARING;
        }
        if(itemStatuses.stream().anyMatch(status -> status == OrderStatus.CONFIRMED)) {
            return OrderStatus.CONFIRMED;
        }
        return OrderStatus.PENDING;
    }

    /** Gộp dòng cùng đơn + cùng món + cùng ghi chú bếp (specialInstructions). */
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

}
