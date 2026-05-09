package com.app.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.app.services.ReservationService;
import com.common.enums.ReservationStatus;
import com.common.models.PaginatedResponse;
import com.common.models.Response;
import com.common.models.reservation.ReservationAdminRequestModel;
import com.common.models.reservation.ReservationCustomerRequestModel;
import com.common.models.reservation.ReservationModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/reservations")
public class ReservationController {
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private LoggingService log;
    @Autowired
    private MessageSource messageSource;

    private LogContext getLogContext(String methodName, List<Integer> reservationIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(reservationIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Response<PaginatedResponse<ReservationModel>>> filtersForCustomer(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer tableNumber,
        @RequestParam(required = false) LocalDateTime reservationTs,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer numberOfGuests,
        @RequestParam(required = false) ReservationStatus reservationStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filtersForCustomer", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext); 

        Page<ReservationModel> reservationPage = reservationService.filtersForCustomer(
            id, tableNumber, reservationTs, numberOfGuests, reservationStatus, pageable
        );
        PaginatedResponse<ReservationModel> paginatedResponse = PaginatedResponse.of(reservationPage);
        Response<PaginatedResponse<ReservationModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetReservationsSuccess", null, locale),
            "reservationModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/filters/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<PaginatedResponse<ReservationModel>>> filtersForAdmin(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) String customerName,
        @RequestParam(required = false) String customerPhone,
        @RequestParam(required = false) String customerEmail,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer tableNumber,
        @RequestParam(required = false) LocalDateTime reservationTs,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer numberOfGuests,
        @RequestParam(required = false) ReservationStatus reservationStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filtersForAdmin", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<ReservationModel> reservationPage = reservationService.filtersForAdmin(
            id, customerName, customerPhone, customerEmail,
            tableNumber, reservationTs, numberOfGuests, reservationStatus, pageable
        );
        PaginatedResponse<ReservationModel> paginatedResponse = PaginatedResponse.of(reservationPage);
        Response<PaginatedResponse<ReservationModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetReservationsByAdminSuccess", null, locale),
            "reservationModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Response<List<ReservationModel>>> create(
        Locale locale,
        @RequestBody @Valid List<ReservationCustomerRequestModel> reservations
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<ReservationModel> created = reservationService.create(reservations);
        Response<List<ReservationModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createReservationsSuccess", null, locale),
            "reservationModel",
            null,
            created
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("{reservationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<Response<ReservationModel>> update(
        Locale locale,
        @RequestBody @Valid ReservationCustomerRequestModel request,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer reservationId
    ) {
        LogContext logContext = getLogContext(
            "update",
            Collections.singletonList(reservationId)
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        ReservationModel updated = reservationService.updateForCustomer(request, reservationId);
        Response<ReservationModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateReservationsSuccess", null, locale),
            "reservationModel",
            null,
            updated
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<ReservationModel>>> updateByAdmin(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<ReservationAdminRequestModel> request
    ) {
        LogContext logContext = getLogContext(
            "updateByAdmin",
            request != null ? request.getIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<ReservationModel> updated = reservationService.updateByAdmin(request.getUpdates(), request.getIds());
        Response<List<ReservationModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateReservationsByAdminSuccess", null, locale),
            "reservationModel",
            null,
            updated
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/cancel/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<ReservationModel>> cancel(
        Locale locale,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer reservationId
    ) {
        LogContext logContext = getLogContext("cancel", Collections.singletonList(reservationId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        ReservationModel cancelled = reservationService.cancel(reservationId);
        Response<ReservationModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.cancelReservationSuccess", null, locale),
            "reservationModel",
            null,
            cancelled
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
