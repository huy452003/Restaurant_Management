package com.app.services.imp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.services.OrderItemStatusSyncService;
import com.app.services.OrderService;
import com.app.services.PaymentService;
import com.app.services.TableStatusSyncService;
import com.app.utils.OrderStatusTransitionUtils;
import com.app.utils.TableHoldGuard;
import com.app.utils.TableLookupUtils;
import com.app.utils.UserEntityUtils;
import com.common.entities.TableEntity;
import com.common.repositories.OrderItemRepository;
import com.common.repositories.OrderRepository;
import com.common.repositories.PaymentRepository;
import com.common.repositories.TableRepository;
import com.common.repositories.UserRepository;
import com.common.enums.PaymentStatus;
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
import com.common.enums.TableStatus;
import com.common.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.modelmapper.ModelMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImp implements OrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoggingService log;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final UserEntityUtils userEntityUtils;
    private final TableRepository tableRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentService paymentService;
    private final OrderItemStatusSyncService orderItemStatusSyncService;
    private final TableStatusSyncService tableStatusSyncService;

    @Value("${order.pending.expiry-minutes:5}")
    private int pendingOrderExpiryMinutes;

    @Value("${order.confirmed.unpaid.expiry-minutes:30}")
    private int confirmedUnpaidExpiryMinutes;

    private LogContext getLogContext(String methodName, List<Integer> orderIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(orderIds)
            .build();
    }

    private static final String ORDER_REDIS_KEY_PREFIX = "order:";
    private static final String TABLE_REDIS_KEY_PREFIX = "table:";

    @Override
    @CircuitBreaker(name = "order-service-read", fallbackMethod = "filtersForCustomerFallback")
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
    @CircuitBreaker(name = "order-service-admin-read", fallbackMethod = "filtersForAdminFallback")
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
            "OrderModel", logContext
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
        applyCanAcceptPayment(pageEntities.getContent(), pageDatas);
        applyAllowedOrderStatuses(pageEntities.getContent(), pageDatas);

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
    @CircuitBreaker(name = "order-service-write", fallbackMethod = "createFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public OrderModel create(OrderCustomerRequestModel order) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext
        );

        OrderEntity orderEntity = modelMapper.map(order, OrderEntity.class);
        orderEntity.setOrderNumber(generateUniqueOrderNumber());
        orderEntity.setCustomerName(currentUser.getFullname());
        orderEntity.setCustomerPhone(currentUser.getPhone());
        orderEntity.setCustomerEmail(currentUser.getEmail());
        assignTableForOrder(orderEntity, order.getOrderType(), order.getTableNumber(), currentUser, logContext);
        orderEntity.setOrderStatus(OrderStatus.PENDING);
        orderEntity.setWaiterId(null);
        orderEntity.setSubTotal(null);
        orderEntity.setTax(null);
        orderEntity.setTotalAmount(null);
        orderEntity.setCompletedAt(null);

        orderRepository.save(orderEntity);

        clearOrderAndTableCaches(logContext);

        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created order with id: " + orderEntity.getId(), logContext);
        return toOrderModel(orderEntity);
    }
    
    @Override
    @CircuitBreaker(name = "order-service-write", fallbackMethod = "updateForCustomerFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public OrderModel updateForCustomer(OrderCustomerRequestModel update, Integer orderId) {
        LogContext logContext = getLogContext(
            "update", Collections.singletonList(orderId)
        );
        log.logInfo("Updating order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext
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

        Integer currentTableNumber = foundOrder.getTable() != null
            ? foundOrder.getTable().getTableNumber() : null;
        Boolean hasChanges = !Objects.equals(update.getTableNumber(), currentTableNumber) ||
                             !Objects.equals(update.getOrderType(), foundOrder.getOrderType()) ||
                             !Objects.equals(update.getNotes(), foundOrder.getNotes());
        if (hasChanges) {
            assignTableForOrder(foundOrder, update.getOrderType(), update.getTableNumber(), currentUser, logContext);
            foundOrder.setOrderType(update.getOrderType());
            foundOrder.setNotes(update.getNotes());
            orderRepository.saveAndFlush(foundOrder);
            log.logInfo("completed, updated order with id: " + orderId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);

        return toOrderModel(foundOrder);
    }
    
    @Override
    @CircuitBreaker(name = "order-service-admin-write", fallbackMethod = "updateByAdminFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<OrderModel> updateByAdmin(List<OrderAdminRequestModel> updates, List<Integer> orderIds) {
        LogContext logContext = getLogContext("updateByAdmin", orderIds);
        log.logInfo("Updating orders by staff ...!", logContext);

        UserEntity actingUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext
        );

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

        List<OrderEntity> ordersToUpdate = new ArrayList<>();
        Set<Integer> tablesToRecheck = new HashSet<>();
        Iterator<OrderAdminRequestModel> orderIterator = updates.iterator();
        Iterator<OrderEntity> currentOrderIterator = foundOrders.iterator();

        while(orderIterator.hasNext() && currentOrderIterator.hasNext()) {
            OrderAdminRequestModel update = orderIterator.next();
            OrderEntity current = currentOrderIterator.next();

            Integer currentWaiterId = current.getWaiter() != null ? current.getWaiter().getId() : null;

            Integer currentTableNumber = current.getTable() != null
                ? current.getTable().getTableNumber() : null;

            Boolean hasChanges = !Objects.equals(update.getCustomerName(), current.getCustomerName()) ||
                                 !Objects.equals(update.getCustomerPhone(), current.getCustomerPhone()) ||
                                 !Objects.equals(update.getCustomerEmail(), current.getCustomerEmail()) ||
                                 !Objects.equals(update.getTableNumber(), currentTableNumber) ||
                                 !Objects.equals(actingUser.getId(), currentWaiterId) ||
                                 !Objects.equals(update.getOrderStatus(), current.getOrderStatus()) ||
                                 !Objects.equals(update.getOrderType(), current.getOrderType()) ||
                                 !Objects.equals(update.getNotes(), current.getNotes());
            if(hasChanges) {
                Integer tableNumberBefore = current.getTable() != null
                    ? current.getTable().getTableNumber() : null;
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
                assignTableForOrder(current, update.getOrderType(), update.getTableNumber(), null, logContext);
                current.setWaiter(actingUser);
                OrderStatusTransitionUtils.applyOrderStatusTransition(
                    current, requestedStatus, hasCompletedPayment(current.getId())
                );
                if (requestedStatus == OrderStatus.CANCELLED && statusBeforeMap != OrderStatus.CANCELLED) {
                    paymentService.cancelPendingPaymentsForOrder(current.getId());
                }
                orderItemStatusSyncService.syncItemsWithOrderStatus(
                    current.getId(), requestedStatus, statusBeforeMap
                );
                if (tableNumberBefore != null) {
                    tablesToRecheck.add(tableNumberBefore);
                }
                if (current.getTable() != null) {
                    tablesToRecheck.add(current.getTable().getTableNumber());
                }
                ordersToUpdate.add(current);
            }
        }

        if(!ordersToUpdate.isEmpty()) {
            orderRepository.saveAllAndFlush(ordersToUpdate);
            tablesToRecheck.forEach(tableStatusSyncService::syncTableStatus);
            log.logInfo("completed, updated " + ordersToUpdate.size() + " orders", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);

        List<OrderModel> orderModels = foundOrders.stream().map(
            this::toOrderModelWithoutItemCount
        ).collect(Collectors.toList());
        applyTotalOrderItemCounts(foundOrders, orderModels);
        applyCanAcceptPayment(foundOrders, orderModels);
        applyAllowedOrderStatuses(foundOrders, orderModels);
        return orderModels;
    }

    @Override
    @CircuitBreaker(name = "order-service-write", fallbackMethod = "submitFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public OrderModel submit(Integer orderId) {
        LogContext logContext = getLogContext("submit", Collections.singletonList(orderId));
        log.logInfo("Submitting order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext
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

        OrderStatus previousSubmitStatus = foundOrder.getOrderStatus();
        OrderStatusTransitionUtils.applyOrderStatusTransition(foundOrder, OrderStatus.CONFIRMED);
        orderItemStatusSyncService.syncItemsWithOrderStatus(
            foundOrder.getId(), OrderStatus.CONFIRMED, previousSubmitStatus
        );
        orderRepository.saveAndFlush(foundOrder);

        if (foundOrder.getOrderType() == OrderType.DINE_IN && foundOrder.getTable() != null) {
            tableStatusSyncService.syncTableStatus(foundOrder.getTable().getTableNumber());
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("completed, submitted order with id: " + orderId, logContext);

        return toOrderModel(foundOrder);
    }

    @Override
    @CircuitBreaker(name = "order-service-write", fallbackMethod = "cancelFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public OrderModel cancel(Integer orderId) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(orderId));
        log.logInfo("Cancelling order ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "OrderModel", logContext
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
        cancelOrderAndSyncTable(foundOrder, logContext);
        log.logInfo("completed, cancelled order with id: " + orderId, logContext);
        return toOrderModel(foundOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public int expireStalePendingOrders() {
        LogContext logContext = getLogContext("expireStalePendingOrders", Collections.emptyList());
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingOrderExpiryMinutes);
        List<OrderEntity> stale = orderRepository.findByOrderStatusAndCreatedAtBefore(
            OrderStatus.PENDING, cutoff
        );
        for (OrderEntity order : stale) {
            cancelOrderAndSyncTable(order, logContext);
            log.logInfo(
                "Expired stale PENDING order id=" + order.getId()
                    + " table=" + (order.getTable() != null ? order.getTable().getTableNumber() : null),
                logContext
            );
        }
        return stale.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public int expireStaleConfirmedUnpaidOrders() {
        LogContext logContext = getLogContext("expireStaleConfirmedUnpaidOrders", Collections.emptyList());
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(confirmedUnpaidExpiryMinutes);

        Set<Integer> orderIds = new LinkedHashSet<>();
        orderIds.addAll(
            orderRepository.findWithStalePendingPaymentAndNoCompleted(
                OrderStatus.CONFIRMED,
                PaymentStatus.PENDING,
                PaymentStatus.COMPLETED,
                cutoff
            ).stream().map(OrderEntity::getId).collect(Collectors.toList())
        );
        orderIds.addAll(
            orderRepository.findConfirmedWithoutPaymentsOlderThan(OrderStatus.CONFIRMED, cutoff)
                .stream().map(OrderEntity::getId).collect(Collectors.toList())
        );
        orderIds.addAll(
            orderRepository.findConfirmedWithOnlyTerminalPaymentsOlderThan(
                OrderStatus.CONFIRMED,
                Arrays.asList(PaymentStatus.PENDING, PaymentStatus.COMPLETED),
                cutoff
            ).stream().map(OrderEntity::getId).collect(Collectors.toList())
        );

        int cancelled = 0;
        for (Integer orderId : orderIds) {
            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            if (order == null || order.getOrderStatus() != OrderStatus.CONFIRMED) {
                continue;
            }
            if (hasCompletedPayment(orderId)) {
                continue;
            }
            cancelOrderAndSyncTable(order, logContext);
            cancelled++;
            log.logInfo(
                "Expired stale CONFIRMED unpaid order id=" + orderId
                    + " orderNumber=" + order.getOrderNumber()
                    + " type=" + order.getOrderType(),
                logContext
            );
        }
        return cancelled;
    }

    // ======================================== Helper Methods ========================================

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

    private OrderModel toOrderModel(OrderEntity orderEntity) {
        OrderModel orderModel = toOrderModelWithoutItemCount(orderEntity);
        orderModel.setTotalOrderItem(orderItemRepository.countByOrder_Id(orderEntity.getId()));
        orderModel.setCanAcceptPayment(resolveCanAcceptPayment(orderEntity, orderModel.getTotalOrderItem()));
        orderModel.setAllowedOrderStatuses(resolveAllowedOrderStatuses(orderEntity));
        return orderModel;
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

    private List<Integer> getOrderIds(List<OrderEntity> orders) {
        return orders.stream().map(OrderEntity::getId).collect(Collectors.toList());
    }

    // setTotalOrderItem cho tất cả các orderModel trong list
    private void applyTotalOrderItemCounts(List<OrderEntity> orders, List<OrderModel> models) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        // lấy tất cả các order id từ các order trong list
        List<Integer> orderIds = getOrderIds(orders);

        Map<Integer, Integer> countsByOrderId = orderItemRepository.countByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(
                key -> (Integer) key[0], // key là order id
                value -> (Integer) value[1] // value là số lượng item của order đó
            ));

        for (int i = 0; i < orders.size(); i++) {
            Integer orderId = orders.get(i).getId();
            // lấy số lượng item của order, nếu không có thì trả về 0
            Integer count = countsByOrderId.getOrDefault(orderId, 0);
            models.get(i).setTotalOrderItem(count);
        }
    }

    // setCanAcceptPayment cho tất cả các orderModel trong list
    private void applyCanAcceptPayment(List<OrderEntity> orders, List<OrderModel> models) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Integer> orderIds = getOrderIds(orders);

        Map<Integer, BigDecimal> allocatedByOrderId = paymentRepository
            // lấy tất cả các order id và số tiền đã allocated của order đó trong status PENDING và COMPLETED
            .sumAllocatedAmountsByOrderIds(orderIds, Arrays.asList(PaymentStatus.PENDING, PaymentStatus.COMPLETED))
            .stream()
            .collect(Collectors.toMap(
                key -> (Integer) key[0],
                value -> (BigDecimal) value[1]
            ));
            
        for (int i = 0; i < orders.size(); i++) {
            OrderEntity order = orders.get(i);
            OrderModel model = models.get(i);
            BigDecimal allocated = allocatedByOrderId.getOrDefault(order.getId(), BigDecimal.ZERO);
            model.setCanAcceptPayment(
                paymentService.canAcceptNewPayment(order, model.getTotalOrderItem(), allocated)
            );
        }
    }

    // setAllowedOrderStatuses cho tất cả các orderModel trong list
    private void applyAllowedOrderStatuses(List<OrderEntity> orders, List<OrderModel> models) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Integer> orderIds = getOrderIds(orders);

        // lấy tất cả các order id có payment status COMPLETED ( order đã thanh toán )
        Set<Integer> paidOrderIds = new HashSet<>(
            paymentRepository.findDistinctOrderIdsByOrderIdInAndPaymentStatus(
                orderIds, PaymentStatus.COMPLETED
            )
        );

        for (int i = 0; i < orders.size(); i++) {
            OrderEntity order = orders.get(i);
            boolean paid = paidOrderIds.contains(order.getId());
            models.get(i).setAllowedOrderStatuses(
                OrderStatusTransitionUtils.allowedTargetStatuses(order.getOrderStatus(), paid)
            );
        }
    }

    // kiểm tra xem đơn có thể nhận thanh toán không
    private Boolean resolveCanAcceptPayment(OrderEntity order, int orderItemCount) {
        BigDecimal allocated = Objects.requireNonNullElse(
            // lấy số tiền đã allocated của order đó trong status PENDING và COMPLETED
            paymentRepository.sumAmountByOrderIdAndPaymentStatuses(
                order.getId(), Arrays.asList(PaymentStatus.PENDING, PaymentStatus.COMPLETED)
            ),
            BigDecimal.ZERO
        );
        return paymentService.canAcceptNewPayment(order, orderItemCount, allocated);
    }

    // lấy tất cả các trạng thái được phép chuyển sang từ trạng thái hiện tại
    private List<OrderStatus> resolveAllowedOrderStatuses(OrderEntity order) {
        return OrderStatusTransitionUtils.allowedTargetStatuses(
            order.getOrderStatus(),
            hasCompletedPayment(order.getId())
        );
    }

    // kiểm tra xem đơn đã thanh toán chưa
    private boolean hasCompletedPayment(Integer orderId) {
        return paymentRepository.existsByOrderIdAndPaymentStatus(orderId, PaymentStatus.COMPLETED);
    }

    private void assignTableForOrder(
        OrderEntity order, OrderType orderType, Integer tableNumber,
        UserEntity customerActor, LogContext logContext
    ) {
        // nếu đơn là đơn giao hàng thì không cần assign bàn
        if (orderType == OrderType.DELIVERY) {
            order.setTable(null);
            return;
        }
        if (tableNumber == null) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Table number is required for dine-in orders",
                Collections.singletonList(order.getId()),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        // kiểm tra xem có đơn nào đang sử dụng bàn này không
        TableHoldGuard.assertNoHoldingOrderOnTable(
            orderRepository, tableNumber, order.getId(), "OrderModel", logContext, log
        );
        // lấy bàn từ database
        TableEntity table = TableLookupUtils.requireTable(
            tableRepository, tableNumber, "OrderModel", logContext, log
        );
        // nếu user hiện tại là customer thì kiểm tra xem bàn có sẵn không
        if (customerActor != null && customerActor.getRole() == UserRole.CUSTOMER) {
            assertTableAvailableForNewOrder(table, logContext);
        }
        order.setTable(table);
    }

    // kiểm tra xem bàn có sẵn không
    private void assertTableAvailableForNewOrder(TableEntity table, LogContext logContext) {
        if (table.getTableStatus() == TableStatus.AVAILABLE) {
            return;
        }
        ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
            "Table is not available for new orders: " + table.getTableNumber()
                + " (status: " + table.getTableStatus() + ")",
            "OrderModel",
            "table must be AVAILABLE"
        );
        log.logError(e.getMessage(), e, logContext);
        throw e;
    }

    // hủy đơn và sync trạng thái order item và bàn
    private void cancelOrderAndSyncTable(OrderEntity foundOrder, LogContext logContext) {
        OrderStatus previousStatus = foundOrder.getOrderStatus();

        // thay đổi trạng thái đơn sang trạng thái CANCELLED
        OrderStatusTransitionUtils.applyOrderStatusTransition(
            foundOrder, OrderStatus.CANCELLED, hasCompletedPayment(foundOrder.getId())
        );

        // sync trạng thái các item của đơn sang trạng thái CANCELLED
        orderItemStatusSyncService.syncItemsWithOrderStatus(
            foundOrder.getId(), OrderStatus.CANCELLED, previousStatus
        );

        orderRepository.saveAndFlush(foundOrder);

        // hủy tất cả các payment pending của đơn
        paymentService.cancelPendingPaymentsForOrder(foundOrder.getId());

        // sync trạng thái bàn
        if (foundOrder.getTable() != null) {
            tableStatusSyncService.syncTableStatus(foundOrder.getTable().getTableNumber());
        }

        clearOrderAndTableCaches(logContext);
    }

    // kiểm tra xem user hiện tại có phải là owner của đơn không
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

    // generate unique order number
    private String generateUniqueOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + System.currentTimeMillis();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private void clearOrderAndTableCaches(LogContext logContext) {
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
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

    // ======================================== Fallback Methods ========================================
    
    @SuppressWarnings("unused")
    private Page<OrderModel> filtersForCustomerFallback(
        Integer id, String orderNumber, Integer tableNumber,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<OrderEntity>> conditions = buildFilterConditions(
            id, orderNumber, tableNumber, null, null, null, null,
            orderStatus, orderType, subTotal, tax, totalAmount
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            ORDER_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<OrderModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, OrderModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo(
                "Found cache when calling fallback filters method, returning...", 
                getLogContext("filtersForCustomer", Collections.emptyList())
            );
            return cachedPage;
        }

        // lỗi circuit breaker open -> throw service unavailable exception
        throw ResilienceFallbackUtils.serviceUnavailable("filtersForCustomer", e);
    }

    @SuppressWarnings("unused")
    private Page<OrderModel> filtersForAdminFallback(
        Integer id, String orderNumber, Integer tableNumber,
        Integer waiterId, String customerName, String customerPhone, String customerEmail,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<OrderEntity>> conditions = buildFilterConditions(
            id, orderNumber, tableNumber, waiterId, customerName, customerPhone, customerEmail,
            orderStatus, orderType, subTotal, tax, totalAmount
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            ORDER_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<OrderModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, OrderModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo(
                "Found cache when calling fallback filters method, returning...", 
                getLogContext("filtersForAdmin", Collections.emptyList())
            );
            return cachedPage;
        }

        // lỗi circuit breaker open -> throw service unavailable exception
        throw ResilienceFallbackUtils.serviceUnavailable("filtersForAdmin", e);
    }

    @SuppressWarnings("unused")
    private OrderModel createFallback(OrderCustomerRequestModel order, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "create");
        return null;
    }

    @SuppressWarnings("unused")
    private OrderModel updateForCustomerFallback(OrderCustomerRequestModel update, Integer orderId, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updateForCustomer");
        return null;
    }

    @SuppressWarnings("unused")
    private List<OrderModel> updateByAdminFallback(List<OrderAdminRequestModel> updates, List<Integer> orderIds, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updateByAdmin");
        return null;
    }

    @SuppressWarnings("unused")
    private OrderModel submitFallback(Integer orderId, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "submit");
        return null;
    }

    @SuppressWarnings("unused")
    private OrderModel cancelFallback(Integer orderId, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "cancel");
        return null;
    }
}
