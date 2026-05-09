package com.app.services.imp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.services.PaymentService;
import com.app.utils.OrderStatusTransitionUtils;
import com.app.utils.UserEntityUtils;
import com.common.entities.OrderEntity;
import com.common.entities.PaymentEntity;
import com.common.entities.UserEntity;
import com.common.enums.OrderStatus;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.enums.UserRole;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;
import com.common.repositories.OrderRepository;
import com.common.repositories.PaymentRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.transaction.Transactional;

@Service
public class PaymentServiceImp implements PaymentService {

    private static final String PAYMENT_REDIS_KEY_PREFIX = "payment:";
    private static final String ORDER_REDIS_KEY_PREFIX = "order:";
    /** Các Payment đếm vào quota so với order total (FAILED/CANCELLED không chiếm hạn mức). */
    private static final List<PaymentStatus> ALLOCATING_STATUSES = Arrays.asList(
        PaymentStatus.PENDING, PaymentStatus.COMPLETED);

    private static final int AMOUNT_SCALE = 2;

    /** Khi đủ tiền, chỉ đóng đơn tự động khi luồng phục vụ đã đến bước gần xong — tránh COMPLETED sai khi chỉ CONFIRMED. */
    private static final EnumSet<OrderStatus> ALLOW_AUTO_COMPLETE_ON_FULL_PAYMENT =
        EnumSet.of(OrderStatus.READY, OrderStatus.SERVED);

    @Autowired
    private PaymentRepository paymentRepository;
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
    private UserEntityUtils userEntityUtils;

    private LogContext getLogContext(String methodName, List<Integer> paymentIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(paymentIds)
            .build();
    }

    @Override
    public Page<PaymentModel> filters(
        Integer id, Integer orderId, Integer cashierId,
        PaymentMethod paymentMethod, BigDecimal amount,
        PaymentStatus paymentStatus, String transactionId,
        Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering payments with pagination ...!", logContext);

        List<FilterCondition<PaymentEntity>> conditions = buildFilterConditions(
            id, orderId, cashierId, paymentMethod,
            amount, paymentStatus, transactionId
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            PAYMENT_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<PaymentModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, PaymentModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " payments in cache", logContext);
            return cached;
        }

        Page<PaymentEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = paymentRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all payments with pagination", logContext);
        } else {
            Specification<PaymentEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = paymentRepository.findAll(spec, pageable);
        }

        List<PaymentModel> pageDatas = pageEntities.getContent().stream().map(this::toPaymentModel)
            .collect(Collectors.toList());

        Page<PaymentModel> paymentModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements());

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, paymentModelPage);
            log.logInfo("cached first-page filter snapshot for " + paymentModelPage.getTotalElements()
                + " payments, key: " + redisKeyFilters, logContext);
        }
        return paymentModelPage;
    }

    @Override
    @Transactional
    public PaymentModel create(PaymentCreateRequestModel paymentRequest) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating payment ...!", logContext);

        UserEntity actor = userEntityUtils.requireAuthenticatedUser("PaymentModel", logContext, log);
        enforceCashierIdMatchesActorIfCashier(actor, paymentRequest.getCashierId(), logContext);

        OrderEntity order = orderRepository.lockByIdForPayment(paymentRequest.getOrderId()).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with id: " + paymentRequest.getOrderId(),
                Collections.singletonList(paymentRequest.getOrderId()),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

        assertOrderAllowsNewPayment(order, logContext);
        BigDecimal amount = normalizeAmount(paymentRequest.getAmount());
        enforceAllocatingCapacity(order, amount, logContext);

        UserEntity cashier = userEntityUtils.requireById(paymentRequest.getCashierId(), "PaymentModel", logContext, log);
        enforceCashierUserRoleIsCashier(cashier, logContext);

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setOrder(order);
        paymentEntity.setCashier(cashier);
        paymentEntity.setPaymentMethod(paymentRequest.getPaymentMethod());
        paymentEntity.setAmount(amount);
        paymentEntity.setPaymentStatus(PaymentStatus.PENDING);
        paymentEntity.setTransactionId(UUID.randomUUID().toString());
        paymentEntity.setPaidAt(null);

        paymentRepository.save(paymentEntity);
        paymentRepository.flush();

        clearPaymentAndOrderCaches(logContext);

        log.logInfo("completed, created payment with id: " + paymentEntity.getId(), logContext);
        return toPaymentModel(paymentEntity);
    }

    @Override
    @Transactional
    public PaymentModel complete(Integer paymentId) {
        LogContext logContext = getLogContext("complete", Collections.singletonList(paymentId));
        log.logInfo("Completing payment ...!", logContext);

        PaymentEntity payment = getPaymentById(paymentId, logContext);
        if (!Objects.equals(payment.getPaymentStatus(), PaymentStatus.PENDING)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only complete pending payments",
                "PaymentModel",
                "payment must be pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        paymentRepository.flush();

        maybeMarkOrderFullyPaid(orderRepository.lockByIdForPayment(resolveOrderId(payment)).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found for payment: " + paymentId,
                Collections.singletonList(paymentId),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        }), logContext);

        clearPaymentAndOrderCaches(logContext);

        return toPaymentModel(payment);
    }

    @Override
    @Transactional
    public PaymentModel cancel(Integer paymentId) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(paymentId));
        log.logInfo("Cancelling payment ...!", logContext);

        PaymentEntity payment = getPaymentById(paymentId, logContext);
        if (!Objects.equals(payment.getPaymentStatus(), PaymentStatus.PENDING)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "You can only cancel pending payments",
                "PaymentModel",
                "payment must be pending"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        clearPaymentAndOrderCaches(logContext);

        log.logInfo("completed, cancelled payment with id: " + paymentId, logContext);
        return toPaymentModel(payment);
    }

    // private method

    private PaymentModel toPaymentModel(PaymentEntity paymentEntity) {
        PaymentModel m = modelMapper.map(paymentEntity, PaymentModel.class);
        if (paymentEntity.getOrder() != null) {
            m.setOrderId(paymentEntity.getOrder().getId());
        }
        if (paymentEntity.getCashier() != null) {
            m.setCashierId(paymentEntity.getCashier().getId());
        }
        return m;
    }

    private PaymentEntity getPaymentById(Integer paymentId, LogContext logContext) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Payment not found with id: " + paymentId,
                Collections.singletonList(paymentId),
                "PaymentModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private Integer resolveOrderId(PaymentEntity payment) {
        if (payment.getOrder() != null) {
            return payment.getOrder().getId();
        }
        Integer id = payment.getOrderId();
        if (id == null) {
            throw new IllegalStateException("Payment has no order reference");
        }
        return id;
    }

    private void maybeMarkOrderFullyPaid(OrderEntity order, LogContext logContext) {
        if (order.getTotalAmount() == null || order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (!ALLOW_AUTO_COMPLETE_ON_FULL_PAYMENT.contains(order.getOrderStatus())) {
            return;
        }
        BigDecimal completedSum = Objects.requireNonNullElse(
            paymentRepository.sumCompletedAmountByOrderId(order.getId()),
            BigDecimal.ZERO
        ).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = order.getTotalAmount().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (completedSum.compareTo(total) == 0) {
            OrderStatusTransitionUtils.applyOrderStatusTransition(order, OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.logInfo("Order " + order.getId() + " marked COMPLETED (payments cover total)", logContext);
        }
    }

    private void enforceCashierIdMatchesActorIfCashier(
        UserEntity actor,
        Integer cashierIdFromRequest,
        LogContext logContext
    ) {
        if (actor.getRole() == UserRole.CASHIER
            && !Objects.equals(actor.getId(), cashierIdFromRequest)) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Cashier may only register payments where cashierId is their own user id",
                "PaymentModel",
                "cashierId must match authenticated cashier"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private void enforceCashierUserRoleIsCashier(UserEntity cashier, LogContext logContext) {
        if (cashier.getRole() != UserRole.CASHIER) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "The payment cashier record must reference a user with role CASHIER",
                "PaymentModel",
                "cashier.role must be CASHIER"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private void assertOrderAllowsNewPayment(OrderEntity order, LogContext logContext) {
        OrderStatus st = order.getOrderStatus();
        if (st == null
            || st == OrderStatus.PENDING
            || st == OrderStatus.CANCELLED
            || st == OrderStatus.COMPLETED) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Order is not payable in its current status: " + st,
                "PaymentModel",
                "orderStatus must not be PENDING, CANCELLED or COMPLETED"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (order.getTotalAmount() == null) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Order total amount is not set; finalize the order before taking payment",
                "PaymentModel",
                "order.totalAmount must be set"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationExceptionHandle(
                "Order total amount must be positive before payment",
                Collections.singletonList(order.getId()),
                "PaymentModel"
            );
        }
    }

    private void enforceAllocatingCapacity(OrderEntity order, BigDecimal newPaymentAmount, LogContext logContext) {
        BigDecimal allocated = Objects.requireNonNullElse(
            paymentRepository.sumAmountByOrderIdAndPaymentStatuses(order.getId(), ALLOCATING_STATUSES),
            BigDecimal.ZERO
        ).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCap = order.getTotalAmount().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        BigDecimal after = allocated.add(newPaymentAmount);
        if (after.compareTo(totalCap) > 0) {
            throw new ValidationExceptionHandle(
                String.format(
                    "Payment would exceed order total (allocated %.2f + new %.2f > %.2f)",
                    allocated, newPaymentAmount, totalCap
                ),
                Collections.singletonList(order.getId()),
                "PaymentModel"
            );
        }
    }

    private static BigDecimal normalizeAmount(BigDecimal raw) {
        return raw.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private void clearPaymentAndOrderCaches(LogContext logContext) {
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, PAYMENT_REDIS_KEY_PREFIX);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("Deleted payment and order filter caches after mutation", logContext);
    }

    private List<FilterCondition<PaymentEntity>> buildFilterConditions(
        Integer id, Integer orderId, Integer cashierId,
        PaymentMethod paymentMethod, BigDecimal amount,
        PaymentStatus paymentStatus, String transactionId
    ) {
        List<FilterCondition<PaymentEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(orderId != null) {
            conditions.add(FilterCondition.eq("orderId", orderId));
        }
        if(cashierId != null) {
            conditions.add(FilterCondition.eq("cashierId", cashierId));
        }
        if(paymentMethod != null) {
            conditions.add(FilterCondition.eq("paymentMethod", paymentMethod));
        }
        if(amount != null) {
            conditions.add(FilterCondition.eq("amount", amount));
        }
        if(paymentStatus != null) {
            conditions.add(FilterCondition.eq("paymentStatus", paymentStatus));
        }
        if(StringUtils.hasText(transactionId)) {
            conditions.add(FilterCondition.likeIgnoreCase("transactionId", transactionId));
        }
        return conditions;
    }
}
