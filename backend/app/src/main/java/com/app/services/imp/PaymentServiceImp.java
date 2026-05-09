package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.services.PaymentService;
import com.common.repositories.OrderRepository;
import com.common.repositories.PaymentRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.entities.PaymentEntity;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

import org.modelmapper.ModelMapper;

@Service
public class PaymentServiceImp implements PaymentService {
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

    private LogContext getLogContext(String methodName, List<Integer> paymentIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(paymentIds)
            .build();
    }

    private static final String PAYMENT_REDIS_KEY_PREFIX = "payment:";

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
        }else {
            Specification<PaymentEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = paymentRepository.findAll(spec, pageable);
        }

        List<PaymentModel> pageDatas = pageEntities.getContent().stream().map(
            paymentEntity -> modelMapper.map(paymentEntity, PaymentModel.class)
        ).collect(Collectors.toList());

        Page<PaymentModel> paymentModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, paymentModelPage);
            log.logInfo("cached first-page filter snapshot for " + paymentModelPage.getTotalElements()
                + " payments, key: " + redisKeyFilters, logContext);
        }
        return paymentModelPage;
    }

    @Override
    public PaymentModel create(PaymentCreateRequestModel payment) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating payment ...!", logContext);

        orderRepository.findById(payment.getOrderId()).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with id: " + payment.getOrderId(),
                Collections.singletonList(payment.getOrderId()),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

        PaymentEntity paymentEntity = modelMapper.map(payment, PaymentEntity.class);
        paymentEntity.setPaymentStatus(PaymentStatus.PENDING);
        paymentEntity.setTransactionId(UUID.randomUUID().toString());
        paymentEntity.setPaidAt(null);

        paymentRepository.save(paymentEntity);

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, PAYMENT_REDIS_KEY_PREFIX);

        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created payment with id: " + paymentEntity.getId(), logContext);
        return modelMapper.map(paymentEntity, PaymentModel.class);
    }
    
    @Override
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
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, PAYMENT_REDIS_KEY_PREFIX);
        return modelMapper.map(payment, PaymentModel.class);
    }

    @Override
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
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, PAYMENT_REDIS_KEY_PREFIX);
        return modelMapper.map(payment, PaymentModel.class);
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
        if(transactionId != null) {
            conditions.add(FilterCondition.likeIgnoreCase("transactionId", transactionId));
        }
        return conditions;
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
}
