package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.services.OrderService;
import com.app.services.PaymentService;
import com.app.utils.OrderStatusTransitionUtils;
import com.app.utils.UserEntityUtils;
import com.common.entities.TableEntity;
import com.common.repositories.OrderItemRepository;
import com.common.repositories.OrderRepository;
import com.common.repositories.TableRepository;
import com.common.repositories.UserRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.models.order.OrderAdminRequestModel;
import com.common.models.order.OrderCustomerRequestModel;
import com.common.models.order.OrderModel;
import com.common.entities.OrderEntity;
import com.common.entities.UserEntity;
import com.common.enums.OrderStatus;
import com.common.enums.OrderType;
import com.common.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

import org.modelmapper.ModelMapper;

@Service
public class OrderServiceImp implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserEntityUtils userEntityUtils;
    @Autowired
    private TableRepository tableRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private PaymentService paymentService;

    private LogContext getLogContext(String methodName, List<Integer> orderIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(orderIds)
            .build();
    }

    private static final String ORDER_REDIS_KEY_PREFIX = "order:";

    @Override
    public Page<OrderModel> filtersForCustomer(
        Integer id, String orderNumber, Integer tableNumber,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable
    ) {
        return filtersInternal(
            id, orderNumber, tableNumber, null, null, null, null,
            orderStatus, orderType, subTotal, tax, totalAmount, pageable,
            "filtersForCustomer"
        );
    }

    @Override
    public Page<OrderModel> filtersForAdmin(
        Integer id, String orderNumber, Integer tableNumber, Integer waiterId,
        String customerName, String customerPhone, String customerEmail,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable
    ) {
        return filtersInternal(
            id, orderNumber, tableNumber, waiterId, customerName, customerPhone, customerEmail,
            orderStatus, orderType, subTotal, tax, totalAmount, pageable,
            "filtersForAdmin"
        );
    }

    private Page<OrderModel> filtersInternal(
        Integer id, String orderNumber, Integer tableNumber, Integer waiterId,
        String customerName, String customerPhone, String customerEmail,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable, String methodName
    ) {
        LogContext logContext = getLogContext(methodName, Collections.emptyList());
        log.logInfo("Filtering orders with pagination ...!", logContext);

        List<FilterCondition<OrderEntity>> conditions = buildFilterConditions(
            id, orderNumber, tableNumber, waiterId, customerName, customerPhone, customerEmail,
            orderStatus, orderType, subTotal, tax, totalAmount
        );
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext, log
        );
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            conditions.add(FilterCondition.eq("customerEmail", currentUser.getEmail()));
        }

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            ORDER_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<OrderModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, OrderModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " orders in cache", logContext);
            return cached;
        }

        Page<OrderEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = orderRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all orders with pagination", logContext);
        }else {
            Specification<OrderEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = orderRepository.findAll(spec, pageable);
        }

        List<OrderModel> pageDatas = pageEntities.getContent().stream().map(
            this::toOrderModelWithoutItemCount
        ).collect(Collectors.toList());
        applyTotalOrderItemCounts(pageEntities.getContent(), pageDatas);

        Page<OrderModel> orderModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, orderModelPage);
            log.logInfo("cached first-page filter snapshot for " + orderModelPage.getTotalElements()
                + " orders, key: " + redisKeyFilters, logContext);
        }
        return orderModelPage;
    }

    @Override
    public OrderModel create(OrderCustomerRequestModel order) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext, log
        );

        OrderEntity orderEntity = modelMapper.map(order, OrderEntity.class);
        orderEntity.setOrderNumber(generateUniqueOrderNumber());
        orderEntity.setCustomerName(currentUser.getFullname());
        orderEntity.setCustomerPhone(currentUser.getPhone());
        orderEntity.setCustomerEmail(currentUser.getEmail());
        orderEntity.setTable(getTable(order.getTableNumber(), logContext));
        orderEntity.setOrderStatus(OrderStatus.PENDING);
        orderEntity.setWaiterId(null);
        orderEntity.setSubTotal(null);
        orderEntity.setTax(null);
        orderEntity.setTotalAmount(null);
        orderEntity.setCompletedAt(null);

        orderRepository.save(orderEntity);

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);

        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created order with id: " + orderEntity.getId(), logContext);
        return toOrderModel(orderEntity);
    }
    
    @Override
    public OrderModel updateForCustomer(OrderCustomerRequestModel update, Integer orderId) {
        LogContext logContext = getLogContext(
            "update", Collections.singletonList(orderId)
        );
        log.logInfo("Updating order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext, log
        );
        OrderEntity foundOrder = getOrder(orderId, logContext);
        orderOwnerCheck(foundOrder, currentUser, logContext);

        if(!Objects.equals(foundOrder.getOrderStatus(), OrderStatus.PENDING)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only update pending orders",
                "OrderModel",
                "order must be pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        Boolean hasChanges = !Objects.equals(update.getTableNumber(), foundOrder.getTable().getTableNumber()) ||
                             !Objects.equals(update.getOrderType(), foundOrder.getOrderType()) ||
                             !Objects.equals(update.getNotes(), foundOrder.getNotes());
        if (hasChanges) {
            foundOrder.setTable(getTable(update.getTableNumber(), logContext));
            foundOrder.setOrderType(update.getOrderType());
            foundOrder.setNotes(update.getNotes());
            orderRepository.save(foundOrder);
            log.logInfo("completed, updated order with id: " + orderId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);

        return toOrderModel(foundOrder);
    }
    
    @Override
    public List<OrderModel> updateByAdmin(List<OrderAdminRequestModel> updates, List<Integer> orderIds) {
        LogContext logContext = getLogContext(
            "updateByAdmin", orderIds != null ? orderIds : Collections.emptyList()
        );
        log.logInfo("Updating orders by admin/manager ...!", logContext);

        if(updates.size() != orderIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and orderIds",
                orderIds,
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<OrderEntity> fetchedOrders = orderRepository.findAllById(orderIds);
        Map<Integer, OrderEntity> ordersById = fetchedOrders.stream()
            .collect(Collectors.toMap(OrderEntity::getId, Function.identity()));
        List<OrderEntity> foundOrders = orderIds.stream().map(id -> {
            OrderEntity order = ordersById.get(id);
            if (order != null) {
                return order;
            }
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with id: " + id,
                Collections.singletonList(id),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }).collect(Collectors.toList());
        log.logInfo("found " + foundOrders.size() + " orders", logContext);

        List<String> customerEmails = updates.stream()
            .map(OrderAdminRequestModel::getCustomerEmail)
            .distinct()
            .collect(Collectors.toList());
        Map<String, UserEntity> customersByEmail = userRepository.findByEmailIn(customerEmails).stream()
            .collect(Collectors.toMap(UserEntity::getEmail, Function.identity()));

        List<Integer> waiterIds = updates.stream()
            .map(OrderAdminRequestModel::getWaiterId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        Map<Integer, UserEntity> waitersById = userRepository.findAllById(waiterIds).stream()
            .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        for (Integer waiterId : waiterIds) {
            UserEntity waiterUser = waitersById.get(waiterId);
            if (waiterUser == null) {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "User not found with id: " + waiterId,
                    Collections.singletonList(waiterId),
                    "OrderModel"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
            if (!isWaiterRole(waiterUser.getRole())) {
                ValidationExceptionHandle e = new ValidationExceptionHandle(
                    "Waiter Id not true, please check again with true waiter",
                    Collections.singletonList(waiterId),
                    "OrderModel"
                );
                log.logError(e.getMessage(), e, logContext);
                throw e;
            }
        }

        List<OrderEntity> ordersToUpdate = new ArrayList<>();
        Iterator<OrderAdminRequestModel> orderIterator = updates.iterator();
        Iterator<OrderEntity> currentOrderIterator = foundOrders.iterator();

        while(orderIterator.hasNext() && currentOrderIterator.hasNext()) {
            OrderAdminRequestModel update = orderIterator.next();
            OrderEntity current = currentOrderIterator.next();

            Boolean hasChanges = !Objects.equals(update.getCustomerName(), current.getCustomerName()) ||
                                 !Objects.equals(update.getCustomerPhone(), current.getCustomerPhone()) ||
                                 !Objects.equals(update.getCustomerEmail(), current.getCustomerEmail()) ||
                                 !Objects.equals(update.getTableNumber(), current.getTable().getTableNumber()) ||
                                 !Objects.equals(update.getWaiterId(), current.getWaiterId()) ||
                                 !Objects.equals(update.getOrderStatus(), current.getOrderStatus()) ||
                                 !Objects.equals(update.getOrderType(), current.getOrderType()) ||
                                 !Objects.equals(update.getNotes(), current.getNotes());
            if(hasChanges) {
                UserEntity customerUser = customersByEmail.get(update.getCustomerEmail());
                if (customerUser == null) {
                    NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                        "User not found with email: " + update.getCustomerEmail(),
                        Collections.singletonList(update.getCustomerEmail()),
                        "OrderModel"
                    );
                    log.logError(e.getMessage(), e, logContext);
                    throw e;
                }

                OrderStatus requestedStatus = update.getOrderStatus();
                OrderStatus statusBeforeMap = current.getOrderStatus();
                modelMapper.map(update, current);
                current.setOrderStatus(statusBeforeMap);
                current.setCustomerName(customerUser.getFullname());
                current.setCustomerPhone(customerUser.getPhone());
                current.setCustomerEmail(customerUser.getEmail());
                current.setTable(getTable(update.getTableNumber(), logContext));
                if (update.getWaiterId() == null) {
                    current.setWaiter(null);
                } else {
                    UserEntity waiterUser = waitersById.get(update.getWaiterId());
                    if (waiterUser == null) {
                        NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                            "User not found with id: " + update.getWaiterId(),
                            Collections.singletonList(update.getWaiterId()),
                            "OrderModel"
                        );
                        log.logError(e.getMessage(), e, logContext);
                        throw e;
                    }
                    current.setWaiter(waiterUser);
                }
                OrderStatusTransitionUtils.applyOrderStatusTransition(current, requestedStatus);
                if (requestedStatus == OrderStatus.CANCELLED && statusBeforeMap != OrderStatus.CANCELLED) {
                    paymentService.cancelPendingPaymentsForOrder(current.getId());
                }
                ordersToUpdate.add(current);
            }
        }

        if(!ordersToUpdate.isEmpty()) {
            orderRepository.saveAll(ordersToUpdate);
            log.logInfo("completed, updated " + ordersToUpdate.size() + " orders", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);

        List<OrderModel> orderModels = foundOrders.stream().map(
            this::toOrderModelWithoutItemCount
        ).collect(Collectors.toList());
        applyTotalOrderItemCounts(foundOrders, orderModels);
        return orderModels;
    }

    @Override
    public OrderModel submit(Integer orderId) {
        LogContext logContext = getLogContext("submit", Collections.singletonList(orderId));
        log.logInfo("Submitting order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext, log
        );
        OrderEntity foundOrder = getOrder(orderId, logContext);
        orderOwnerCheck(foundOrder, currentUser, logContext);

        if(foundOrder.getOrderStatus() != OrderStatus.PENDING) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only submit pending orders",
                "OrderModel",
                "order must be pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        if(!orderItemRepository.existsByOrder_Id(orderId)) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Order must have at least one order item before submit",
                Collections.singletonList(orderId),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        OrderStatusTransitionUtils.applyOrderStatusTransition(foundOrder, OrderStatus.CONFIRMED);
        orderRepository.save(foundOrder);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("completed, submitted order with id: " + orderId, logContext);

        return toOrderModel(foundOrder);
    }
        
    @Override
    public OrderModel cancel(Integer orderId) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(orderId));
        log.logInfo("Cancelling order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext, log
        );
        OrderEntity foundOrder = getOrder(orderId, logContext);
        orderOwnerCheck(foundOrder, currentUser, logContext);

        if (foundOrder.getOrderStatus() != OrderStatus.CONFIRMED &&
            foundOrder.getOrderStatus() != OrderStatus.PENDING) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only cancel confirmed or pending orders",
                "OrderModel",
                "order must be confirmed or pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        OrderStatusTransitionUtils.applyOrderStatusTransition(foundOrder, OrderStatus.CANCELLED);
        orderRepository.save(foundOrder);
        paymentService.cancelPendingPaymentsForOrder(foundOrder.getId());
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("completed, cancelled order with id: " + orderId, logContext);

        return toOrderModel(foundOrder);
    }

    // private method

    private OrderModel toOrderModel(OrderEntity orderEntity) {
        OrderModel orderModel = toOrderModelWithoutItemCount(orderEntity);
        orderModel.setTotalOrderItem(Math.toIntExact(orderItemRepository.countByOrder_Id(orderEntity.getId())));
        return orderModel;
    }

    private OrderModel toOrderModelWithoutItemCount(OrderEntity orderEntity) {
        OrderModel orderModel = modelMapper.map(orderEntity, OrderModel.class);
        if(orderEntity.getTable() != null) {
            orderModel.setTableNumber(orderEntity.getTable().getTableNumber());
        }
        if(orderEntity.getWaiter() != null) {
            orderModel.setWaiterId(orderEntity.getWaiter().getId());
        }
        orderModel.setTotalOrderItem(0);
        return orderModel;
    }

    private void applyTotalOrderItemCounts(List<OrderEntity> orders, List<OrderModel> models) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Integer> orderIds = orders.stream()
            .map(OrderEntity::getId)
            .collect(Collectors.toList());
        Map<Integer, Long> countsByOrderId = orderItemRepository.countByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(
                row -> (Integer) row[0],
                row -> (Long) row[1]
            ));
        for (int i = 0; i < orders.size(); i++) {
            Integer orderId = orders.get(i).getId();
            long count = countsByOrderId.getOrDefault(orderId, 0L);
            models.get(i).setTotalOrderItem(Math.toIntExact(count));
        }
    }

    private OrderEntity getOrder(Integer orderId, LogContext logContext) {
        return orderRepository.findById(orderId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with id: " + orderId,
                Collections.singletonList(orderId),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private TableEntity getTable(Integer tableNumber, LogContext logContext) {
        return tableRepository.findByTableNumber(tableNumber).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Table not found with tableNumber: " + tableNumber,
                Collections.singletonList(tableNumber),
                "TableModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private void orderOwnerCheck(OrderEntity order, UserEntity currentUser, LogContext logContext) {
        if (currentUser.getRole() == UserRole.CUSTOMER &&
            !Objects.equals(order.getCustomerEmail(), currentUser.getEmail())) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only update/submit/cancel your own orders",
                "OrderModel",
                "order owner must match authenticated customer"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private String generateUniqueOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + System.currentTimeMillis();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private boolean isWaiterRole(UserRole role) {
        return Objects.equals(role, UserRole.WAITER);
    }

    private List<FilterCondition<OrderEntity>> buildFilterConditions(
        Integer id, String orderNumber, Integer tableNumber, Integer waiterId,
        String customerName, String customerPhone, String customerEmail,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount
    ) {
        List<FilterCondition<OrderEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(StringUtils.hasText(orderNumber)) {
            conditions.add(FilterCondition.likeIgnoreCase("orderNumber", orderNumber));
        }
        if(tableNumber != null) {
            conditions.add(FilterCondition.eq("table.tableNumber", tableNumber));
        }
        if(waiterId != null) {
            conditions.add(FilterCondition.eq("waiterId", waiterId));
        }
        if(StringUtils.hasText(customerName)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerName", customerName));
        }
        if(StringUtils.hasText(customerPhone)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerPhone", customerPhone));
        }
        if(StringUtils.hasText(customerEmail)) {
            conditions.add(FilterCondition.likeIgnoreCase("customerEmail", customerEmail));
        }
        if(orderStatus != null) {
            conditions.add(FilterCondition.eq("orderStatus", orderStatus));
        }
        if(orderType != null) {
            conditions.add(FilterCondition.eq("orderType", orderType));
        }
        if(subTotal != null) {
            conditions.add(FilterCondition.eq("subTotal", subTotal));
        }
        if(tax != null) {
            conditions.add(FilterCondition.eq("tax", tax));
        }
        if(totalAmount != null) {
            conditions.add(FilterCondition.eq("totalAmount", totalAmount));
        }
        return conditions;
    }

}
