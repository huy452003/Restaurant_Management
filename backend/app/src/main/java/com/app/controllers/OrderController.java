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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.app.services.OrderService;
import com.common.enums.OrderStatus;
import com.common.enums.OrderType;
import com.common.models.PaginatedResponse;
import com.common.models.order.OrderAdminRequestModel;
import com.common.models.order.OrderCustomerRequestModel;
import com.common.models.order.OrderModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.common.models.Response;
import com.logging.services.LoggingService;
import com.logging.models.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Validated
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> orderIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(orderIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Response<PaginatedResponse<OrderModel>>> filtersForCustomer(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) String orderNumber,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer tableNumber,
        @RequestParam(required = false) OrderStatus orderStatus,
        @RequestParam(required = false) OrderType orderType,
        @RequestParam(required = false) BigDecimal subTotal,
        @RequestParam(required = false) BigDecimal tax,
        @RequestParam(required = false) BigDecimal totalAmount,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filtersForCustomer", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<OrderModel> orderPage = orderService.filtersForCustomer(
            id, orderNumber, tableNumber,
            orderStatus, orderType,
            subTotal, tax, totalAmount, pageable
        );
        PaginatedResponse<OrderModel> paginatedResponse = PaginatedResponse.of(orderPage);
        Response<PaginatedResponse<OrderModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetOrdersSuccess", null, locale),
            "orderModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/filters/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<PaginatedResponse<OrderModel>>> filtersForAdmin(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) String orderNumber,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer tableNumber,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer waiterId,
        @RequestParam(required = false) String customerName,
        @RequestParam(required = false) String customerPhone,
        @RequestParam(required = false) String customerEmail,
        @RequestParam(required = false) OrderStatus orderStatus,
        @RequestParam(required = false) OrderType orderType,
        @RequestParam(required = false) BigDecimal subTotal,
        @RequestParam(required = false) BigDecimal tax,
        @RequestParam(required = false) BigDecimal totalAmount,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filtersForAdmin", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<OrderModel> orderPage = orderService.filtersForAdmin(
            id, orderNumber, tableNumber, waiterId,
            customerName, customerPhone, customerEmail,
            orderStatus, orderType, subTotal, tax, totalAmount, pageable
        );
        PaginatedResponse<OrderModel> paginatedResponse = PaginatedResponse.of(orderPage);
        Response<PaginatedResponse<OrderModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetOrdersSuccess", null, locale),
            "orderModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<OrderModel>> create(
        Locale locale,
        @RequestBody @Valid OrderCustomerRequestModel order
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        OrderModel createdOrder = orderService.create(order);
        Response<OrderModel> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createOrdersSuccess", null, locale),
            "orderModel",
            null,
            createdOrder
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }


    @PatchMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<Response<OrderModel>> update(
        Locale locale,
        @RequestBody @Valid OrderCustomerRequestModel request,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer orderId
    ) {
        LogContext logContext = getLogContext("update", Collections.singletonList(orderId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        OrderModel updatedOrder = orderService.updateForCustomer(request, orderId);
        Response<OrderModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateOrdersSuccess", null, locale),
            "orderModel",
            null,
            updatedOrder
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<OrderModel>>> updateByAdmin(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<OrderAdminRequestModel> request
    ) {
        LogContext logContext = getLogContext(
            "updateByAdmin", request != null ? request.getIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<OrderModel> updatedOrders = orderService.updateByAdmin(request.getUpdates(), request.getIds());
        Response<List<OrderModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateOrdersSuccess", null, locale),
            "orderModel",
            null,
            updatedOrders
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }


    @PatchMapping("/submit/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<OrderModel>> submit(
        Locale locale,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}")
        Integer orderId
    ) {
        LogContext logContext = getLogContext("submit", Collections.singletonList(orderId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        OrderModel submittedOrder = orderService.submit(orderId);
        Response<OrderModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.submitOrderSuccess", null, locale),
            "orderModel",
            null,
            submittedOrder
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/cancel/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<OrderModel>> cancel(
        Locale locale,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}")
        Integer orderId
    ) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(orderId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        OrderModel cancelled = orderService.cancel(orderId);
        Response<OrderModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.cancelOrderSuccess", null, locale),
            "orderModel",
            null,
            cancelled
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
