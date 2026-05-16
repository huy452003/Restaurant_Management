package com.app.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.app.services.TableService;
import com.common.enums.TableStatus;
import com.common.models.PaginatedResponse;
import com.common.models.Response;
import com.common.models.table.TableModel;
import com.common.models.table.TableRequestModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/tables")
public class TableController {
    @Autowired
    private TableService tableService;
    @Autowired
    private LoggingService log;
    @Autowired
    private MessageSource messageSource;

    private LogContext getLogContext(String methodName, List<Integer> tableIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(tableIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<PaginatedResponse<TableModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.table.tableNumber.min}") Integer tableNumber,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.table.capacity.min}") Integer capacity,
        @RequestParam(required = false) TableStatus tableStatus,
        @RequestParam(required = false) String location,
        @RequestParam(required = false, defaultValue = "false") boolean excludeTablesWithPendingOrder,
        @RequestParam(required = false, defaultValue = "false") boolean freshSnapshot,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext); 

        Page<TableModel> tablePage = tableService.filters(
            id, tableNumber, capacity, tableStatus, location, excludeTablesWithPendingOrder,
            freshSnapshot, pageable
        );
        PaginatedResponse<TableModel> paginatedResponse = PaginatedResponse.of(tablePage);
        Response<PaginatedResponse<TableModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetTablesSuccess", null, locale),
            "tableModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<TableModel>>> create(
        Locale locale,
        @RequestBody @Valid List<TableRequestModel> tables
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<TableModel> created = tableService.create(tables);
        Response<List<TableModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createTablesSuccess", null, locale),
            "tableModel",
            null,
            created
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<TableModel>>> update(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<TableRequestModel> request
    ) {
        LogContext logContext = getLogContext(
            "update", 
            request != null ? request.getIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<TableModel> updated = tableService.update(request.getUpdates(), request.getIds());
        Response<List<TableModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateTablesSuccess", null, locale),
            "tableModel",
            null,
            updated
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/status/available/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Response<TableModel>> markAvailable(
        Locale locale,
        @PathVariable @NotNull @Min(1) Integer tableId
    ) {
        LogContext logContext = getLogContext("markAvailable", Collections.singletonList(tableId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        TableModel model = tableService.markAvailable(tableId);
        Response<TableModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateTablesSuccess", null, locale),
            "tableModel",
            null,
            model
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

}
