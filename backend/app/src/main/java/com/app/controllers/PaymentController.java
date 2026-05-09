package com.app.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.app.services.PaymentService;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.PaginatedResponse;
import com.common.models.Response;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;
import com.logging.services.LoggingService;
import com.logging.models.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> paymentIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(paymentIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Response<PaginatedResponse<PaymentModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer orderId,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer cashierId,
        @RequestParam(required = false) PaymentMethod paymentMethod,
        @RequestParam(required = false) BigDecimal amount,
        @RequestParam(required = false) PaymentStatus paymentStatus,
        @RequestParam(required = false) String transactionId,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<PaymentModel> paymentPage = paymentService.filters(
            id, orderId, cashierId, paymentMethod, 
            amount, paymentStatus, transactionId, pageable
        );
        PaginatedResponse<PaymentModel> paginatedResponse = PaginatedResponse.of(paymentPage);
        Response<PaginatedResponse<PaymentModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetPaymentsSuccess", null, locale),
            "paymentModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    @PostMapping("")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Response<PaymentModel>> create(
        Locale locale,
        @RequestBody @Valid PaymentCreateRequestModel payment
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        PaymentModel createdPayment = paymentService.create(payment);
        Response<PaymentModel> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createPaymentSuccess", null, locale),
            "paymentModel",
            null,
            createdPayment
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }


    @PatchMapping("/complete/{paymentId}")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Response<PaymentModel>> complete(
        Locale locale,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer paymentId
    ) {
        LogContext logContext = getLogContext("complete", Collections.singletonList(paymentId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        PaymentModel completedPayment = paymentService.complete(paymentId);
        Response<PaymentModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.completePaymentSuccess", null, locale),
            "paymentModel",
            null,
            completedPayment
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/cancel/{paymentId}")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Response<PaymentModel>> cancel(
        Locale locale,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer paymentId
    ) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(paymentId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        PaymentModel cancelledPayment = paymentService.cancel(paymentId);
        Response<PaymentModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.cancelPaymentSuccess", null, locale),
            "paymentModel",
            null,
            cancelledPayment
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
