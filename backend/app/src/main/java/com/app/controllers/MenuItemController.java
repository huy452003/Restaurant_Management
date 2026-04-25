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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;

import com.app.services.MenuItemService;
import com.common.enums.MenuItemStatus;
import com.common.models.PaginatedResponse;
import com.common.models.menu.MenuItemModel;
import com.common.models.wrapper.UpdateMenuItemRequest;
import com.common.models.Response;
import com.logging.services.LoggingService;
import com.logging.models.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {
    @Autowired
    private MenuItemService menuItemService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> menuItemIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(menuItemIds)
            .build();
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<PaginatedResponse<MenuItemModel>>> filters(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestParam(required = false) Integer id,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String categoryName,
        @RequestParam(required = false) MenuItemStatus menuItemStatus,
        @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        Page<MenuItemModel> menuItemPage = menuItemService.filters(id, name, categoryName, menuItemStatus, pageable);
        PaginatedResponse<MenuItemModel> paginatedResponse = PaginatedResponse.of(menuItemPage);
        Response<PaginatedResponse<MenuItemModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filtersSuccess", null, locale),
            "menuItemModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<MenuItemModel>>> create(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid List<MenuItemModel> menuItems
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<MenuItemModel> createdMenuItems = menuItemService.create(menuItems);
        Response<List<MenuItemModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createSuccess", null, locale),
            "menuItemModel",
            null,
            createdMenuItems
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }


    @PutMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<MenuItemModel>>> update(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateMenuItemRequest request
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("update", request != null ? request.getMenuItemIds() : Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<MenuItemModel> updatedMenuItems = menuItemService.update(request.getUpdates(), request.getMenuItemIds());
        Response<List<MenuItemModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateForAdminSuccess", null, locale),
            "menuItemModel",
            null,
            updatedMenuItems
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
