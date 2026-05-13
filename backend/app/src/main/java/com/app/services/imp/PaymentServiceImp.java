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
        Integer id, String orderNumber, String cashierFullname,
        PaymentMethod paymentMethod, BigDecimal amount,
        PaymentStatus paymentStatus, String transactionId,
        Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering payments with pagination ...!", logContext);

        List<FilterCondition<PaymentEntity>> conditions = buildFilterConditions(
            id, orderNumber, cashierFullname, paymentMethod,
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

        OrderEntity order = resolveOrderByOrderNumber(paymentRequest.getOrderNumber(), logContext);
        orderRepository.lockByIdForPayment(order.getId());

        UserEntity cashier = userEntityUtils.requireAuthenticatedUser("PaymentModel", logContext, log);

        assertOrderAllowsNewPayment(order, logContext);
        BigDecimal amount = computeRemainingPayable(order, logContext);

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setOrder(order);
        paymentEntity.setCashier(cashier);
        paymentEntity.setPaymentMethod(paymentRequest.getPaymentMethod());
        paymentEntity.setAmount(amount);
        paymentEntity.setPaymentStatus(PaymentStatus.PENDING);
        paymentEntity.setTransactionId(UUID.randomUUID().toString());
        paymentEntity.setPaidAt(null);

        paymentRepository.save(paymentEntity);
        if(paymentEntity.getId() != null && paymentEntity.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentEntity.setTransactionId(String.valueOf(paymentEntity.getId()));
        }
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

        OrderEntity order = resolveOrderByOrderNumber(payment.getOrder().getOrderNumber(), logContext);
        orderRepository.lockByIdForPayment(order.getId());
        maybeMarkOrderFullyPaid(order, logContext);

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

    @Override
    @Transactional
    public void cancelPendingPaymentsForOrder(Integer orderId) {
        LogContext logContext = getLogContext("cancelPendingPaymentsForOrder", Collections.singletonList(orderId));
        List<PaymentEntity> pending = paymentRepository.findByOrder_IdAndPaymentStatus(orderId, PaymentStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (PaymentEntity p : pending) {
            p.setPaymentStatus(PaymentStatus.CANCELLED);
            p.setPaidAt(now);
        }
        paymentRepository.saveAll(pending);
        paymentRepository.flush();
        clearPaymentAndOrderCaches(logContext);
        log.logInfo("cancelled " + pending.size() + " pending payment(s) for order " + orderId, logContext);
    }

    @Override
    @Transactional
    public PaymentModel markFailedFromGateway(Integer paymentId) {
        // Đặt trạng thái FAILED cho giao dịch chờ (PENDING) sau khi VNPAY từ chối / lỗi — giữ idempotency ở lớp gọi.
        LogContext logContext = getLogContext("markFailedFromGateway", Collections.singletonList(paymentId));
        log.logInfo("Marking payment FAILED from gateway ...!", logContext);

        PaymentEntity payment = getPaymentById(paymentId, logContext);
        if (!Objects.equals(payment.getPaymentStatus(), PaymentStatus.PENDING)) {
            log.logWarn("skip mark FAILED — payment " + paymentId + " not PENDING", logContext);
            return toPaymentModel(payment);
        }

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        paymentRepository.flush();

        clearPaymentAndOrderCaches(logContext);
        log.logInfo("completed, payment " + paymentId + " marked FAILED", logContext);
        return toPaymentModel(payment);
    }

    // private method

    private PaymentModel toPaymentModel(PaymentEntity paymentEntity) {
        PaymentModel paymentModel = modelMapper.map(paymentEntity, PaymentModel.class);
        if (paymentEntity.getOrder() != null) {
            paymentModel.setOrderNumber(paymentEntity.getOrder().getOrderNumber());
        }
        if (paymentEntity.getCashier() != null) {
            paymentModel.setCashierFullname(paymentEntity.getCashier().getFullname());
        }
        return paymentModel;
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

    private OrderEntity resolveOrderByOrderNumber(String orderNumber, LogContext logContext) {
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

    private void maybeMarkOrderFullyPaid(OrderEntity order, LogContext logContext) {
        // bỏ qua nếu totalAmount is null hoặc orderStatus is CANCELLED
        if (order.getTotalAmount() == null || order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }
        // bỏ qua nếu status không phải là READY hoặc SERVED
        if (!ALLOW_AUTO_COMPLETE_ON_FULL_PAYMENT.contains(order.getOrderStatus())) {
            return;
        }
        // lấy tổng của các payment đã được completed
        BigDecimal completedSum = Objects.requireNonNullElse(
            paymentRepository.sumCompletedAmountByOrderId(order.getId()),
            BigDecimal.ZERO
        ).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        // lấy tổng amount của đơn
        BigDecimal total = order.getTotalAmount().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        // nếu completedSum bằng tổng amount của đơn thì đóng đơn
        if (completedSum.compareTo(total) == 0) {
            OrderStatusTransitionUtils.applyOrderStatusTransition(order, OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.logInfo("Order " + order.getId() + " marked COMPLETED (payments cover total)", logContext);
        }
    }

    // Kiểm tra xem đơn có thể nhận thanh toán không
    private void assertOrderAllowsNewPayment(OrderEntity order, LogContext logContext) {
        OrderStatus orderStatus = order.getOrderStatus();
        if (
            orderStatus == null ||
            orderStatus == OrderStatus.PENDING ||
            orderStatus == OrderStatus.CANCELLED ||
            orderStatus == OrderStatus.COMPLETED
        ) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Order is not payable in its current status: " + orderStatus,
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

    /** Số tiền còn lại của đơn = total − Σ(PENDING + COMPLETED); server tự tính để tránh nhập sai/tampering. */
    private BigDecimal computeRemainingPayable(OrderEntity order, LogContext logContext) {
        // lấy tổng amount của các payment ( lần đầu khi init payment sẽ luôn là 0 vì chưa có payment nào )
        BigDecimal allocated = Objects.requireNonNullElse(
            paymentRepository.sumAmountByOrderIdAndPaymentStatuses(order.getId(), ALLOCATING_STATUSES),
            BigDecimal.ZERO
        ).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        // lấy tổng amount của đơn
        BigDecimal totalCap = order.getTotalAmount().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        // nếu đã tồn tại payment đó rồi thì sẽ lấy totalAmountPayment - totalAmountOrder = 0, throw error
        // nên sẽ không có payment nào bị trùng với trang thái pending bị dư thừa làm lệch tính tổng payment được ghi nhận
        // look order chỉ giải quyết khi bất đồng bộ còn khi đồng bộ thì đây sẽ là bảo hiểm
        BigDecimal remaining = totalCap.subtract(allocated); // totalCap - allocated
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "No remaining amount to pay (order fully allocated or has PENDING payment)",
                Collections.singletonList(order.getId()),
                "PaymentModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        return remaining;
    }

    private void clearPaymentAndOrderCaches(LogContext logContext) {
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, PAYMENT_REDIS_KEY_PREFIX);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, ORDER_REDIS_KEY_PREFIX);
        log.logInfo("Deleted payment and order filter caches after mutation", logContext);
    }

    private List<FilterCondition<PaymentEntity>> buildFilterConditions(
        Integer id, String orderNumber, String cashierFullname,
        PaymentMethod paymentMethod, BigDecimal amount,
        PaymentStatus paymentStatus, String transactionId
    ) {
        List<FilterCondition<PaymentEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(StringUtils.hasText(orderNumber)) {
            conditions.add(FilterCondition.likeIgnoreCase("order.orderNumber", orderNumber));
        }
        if(StringUtils.hasText(cashierFullname)) {
            conditions.add(FilterCondition.likeIgnoreCase("cashier.fullname", cashierFullname));
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
