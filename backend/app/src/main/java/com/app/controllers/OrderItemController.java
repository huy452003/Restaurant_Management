package com.app.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

import com.app.services.OrderItemService;
import com.common.enums.OrderStatus;
import com.common.models.PaginatedResponse;
import com.common.models.orderItem.OrderItemAdminUpdateModel;
import com.common.models.orderItem.OrderItemCreateModel;
import com.common.models.orderItem.OrderItemCustomerUpdateModel;
import com.common.models.orderItem.OrderItemModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.common.models.Response;
import com.logging.services.LoggingService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.logging.models.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/order-items")
public class OrderItemController {
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> orderItemIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(orderItemIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "controller-public-read", fallbackMethod = "filtersForCustomerFallback")
    public ResponseEntity<Response<PaginatedResponse<OrderItemModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) String orderNumber,
        @RequestParam(required = false) OrderStatus orderItemStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<OrderItemModel> orderItemPage = orderItemService.filters(
            id, orderNumber, orderItemStatus, pageable
        );
        PaginatedResponse<OrderItemModel> paginatedResponse = PaginatedResponse.of(orderItemPage);
        Response<PaginatedResponse<OrderItemModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetOrderItemsSuccess", null, locale),
            "orderItemModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "controller-public-write", fallbackMethod = "createFallback")
    public ResponseEntity<Response<List<OrderItemModel>>> create(
        Locale locale,
        @RequestBody @Valid List<OrderItemCreateModel> orderItems
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<OrderItemModel> createdOrderItems = orderItemService.create(orderItems);
        Response<List<OrderItemModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createOrderItemsSuccess", null, locale),
            "orderItemModel",
            null,
            createdOrderItems
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{orderItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @RateLimiter(name = "order-item-controller-customer-write", fallbackMethod = "updateForCustomerFallback")
    public ResponseEntity<Response<OrderItemModel>> updateForCustomer(
        Locale locale,
        @RequestBody @Valid OrderItemCustomerUpdateModel request,
        @PathVariable @Min(value = 1, message = "{validate.param.id.min}") Integer orderItemId
    ) {
        LogContext logContext = getLogContext("updateForCustomer", Collections.singletonList(orderItemId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        OrderItemModel updatedOrderItem = orderItemService.updateForCustomer(request, orderItemId);
        Response<OrderItemModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateOrderItemsSuccess", null, locale),
            "orderItemModel",
            null,
            updatedOrderItem
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @RateLimiter(name = "order-item-controller-staff-write", fallbackMethod = "updateByAdminFallback")
    public ResponseEntity<Response<List<OrderItemModel>>> updateByAdmin(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<OrderItemAdminUpdateModel> request
    ) {
        LogContext logContext = getLogContext("update", request != null ? request.getIds() : Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<OrderItemModel> updatedOrderItems = orderItemService.updateByAdmin(request.getUpdates(), request.getIds());
        Response<List<OrderItemModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateOrderItemsSuccess", null, locale),
            "orderItemModel",
            null,
            updatedOrderItems
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ======================================== Fallback Methods ========================================

    @SuppressWarnings("unused")
    private ResponseEntity<Response<PaginatedResponse<OrderItemModel>>> filtersForCustomerFallback(
        Locale locale, Integer id, String orderNumber, 
        OrderStatus orderItemStatus, Pageable pageable, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "filtersForCustomer");
        return null;
    }

    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<OrderItemModel>>> createFallback(
        Locale locale, List<OrderItemCreateModel> orderItems, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "create");
        return null;
    }

    @SuppressWarnings("unused")
    private ResponseEntity<Response<OrderItemModel>> updateForCustomerFallback(
        Locale locale, OrderItemCustomerUpdateModel request, Integer orderItemId, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "updateForCustomer");
        return null;
    }

    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<OrderItemModel>>> updateByAdminFallback(
        Locale locale, WrapperUpdateRequest<OrderItemAdminUpdateModel> request, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "updateByAdmin");
        return null;
    }
}
