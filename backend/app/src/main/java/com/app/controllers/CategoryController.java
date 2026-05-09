package com.app.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.app.services.CategoryService;
import com.common.enums.CategoryStatus;
import com.common.models.PaginatedResponse;
import com.common.models.Response;
import com.common.models.category.CategoryModel;
import com.common.models.wrapper.WrapperUpdateRequest;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

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
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private LoggingService log;
    @Autowired
    private MessageSource messageSource;

    private LogContext getLogContext(String methodName, List<Integer> categoryIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(categoryIds)
            .build();
    }

    @GetMapping("/filters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<PaginatedResponse<CategoryModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) CategoryStatus categoryStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext); 

        Page<CategoryModel> categoryPage = categoryService.filters(
            id, name, categoryStatus, pageable
        );
        PaginatedResponse<CategoryModel> paginatedResponse = PaginatedResponse.of(categoryPage);
        Response<PaginatedResponse<CategoryModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filterAndGetCategoriesSuccess", null, locale),
            "categoryModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<CategoryModel>>> create(
        Locale locale,
        @RequestBody @Valid List<CategoryModel> categories
    ) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<CategoryModel> created = categoryService.create(categories);
        Response<List<CategoryModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createCategoriesSuccess", null, locale),
            "categoryModel",
            null,
            created
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Response<List<CategoryModel>>> update(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<CategoryModel> request
    ) {
        LogContext logContext = getLogContext(
            "update", 
            request != null ? request.getIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<CategoryModel> updated = categoryService.update(request.getUpdates(), request.getIds());
        Response<List<CategoryModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateCategoriesSuccess", null, locale),
            "categoryModel",
            null,
            updated
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

}
