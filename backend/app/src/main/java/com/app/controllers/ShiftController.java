package com.app.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

import com.app.services.ShiftService;
import com.common.enums.ShiftStatus;
import com.common.models.PaginatedResponse;
import com.common.models.user.ShiftModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.common.models.Response;
import com.logging.services.LoggingService;
import com.logging.models.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/shifts")
public class ShiftController {
    @Autowired
    private ShiftService shiftService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> shiftIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(shiftIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','CHEF','CASHIER')")
    public ResponseEntity<Response<PaginatedResponse<ShiftModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) LocalDate shiftDate,
        @RequestParam(required = false) LocalDateTime startTime,
        @RequestParam(required = false) LocalDateTime endTime,
        @RequestParam(required = false) ShiftStatus shiftStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<ShiftModel> shiftPage = shiftService.filters(
            id, shiftDate, startTime, endTime, shiftStatus, pageable
        );
        PaginatedResponse<ShiftModel> paginatedResponse = PaginatedResponse.of(shiftPage);
        Response<PaginatedResponse<ShiftModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetShiftsSuccess", null, locale),
            "shiftModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    @PostMapping("")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Response<List<ShiftModel>>> create(
        Locale locale,
        @RequestBody @Valid List<ShiftModel> shifts
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<ShiftModel> createdShifts = shiftService.create(shifts);
        Response<List<ShiftModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createShiftsSuccess", null, locale),
            "shiftModel",
            null,
            createdShifts
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }


    @PutMapping("")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Response<List<ShiftModel>>> update(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<ShiftModel> request
    ) {
        LogContext logContext = getLogContext("update", request != null ? request.getIds() : Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<ShiftModel> updatedShifts = shiftService.update(request.getUpdates(), request.getIds());
        Response<List<ShiftModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateShiftsSuccess", null, locale),
            "shiftModel",
            null,
            updatedShifts
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
