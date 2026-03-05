package com.handle_exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.common.models.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(1) // Độ ưu tiên cao để catch DataIntegrityViolationException trước handler Exception tổng quát
public class CustomExceptionHandler {
    @Autowired
    MessageSource messageSource;
    
    // Not Found Exception Handler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundExceptionHandle.class)
    ResponseEntity<Response<?>> notFoundExceptionHandle(NotFoundExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put("notFoundItems", e.getNotFounds());
        errors.put("detailMessage", e.getMessage());

        Response<?> response = new Response<>(
            404,
            messageSource.getMessage("response.error.notFoundError", null, locale),
            e.getModelName(),
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Conflict Exception Handler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictExceptionHandle.class)
    ResponseEntity<Response<?>> conflictExceptionHandle(ConflictExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put("conflictItems", e.getConflicts());
        errors.put("detailMessage", e.getMessage());

        Response<?> response = new Response<>(
            409,
            messageSource.getMessage("response.error.conflictError", null, locale),
            e.getModelName(),
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Data Integrity Violation Exception Handler (Duplicate unique constraint)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Response<?>> dataIntegrityViolationExceptionHandle(DataIntegrityViolationException e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        String errorMessage = e.getMessage();
        String rootCauseMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : null;
        String fullMessage = rootCauseMessage != null ? rootCauseMessage : errorMessage;
        
        String duplicateField = "uniqueField";
        String duplicateValue = null;
        
        if (fullMessage != null) {
            // Định dạng: "Duplicate entry 'value' cho key 'table.UK...'" (MySQL auto-generated constraint names)
            if (fullMessage.contains("Duplicate entry")) {
                // Trích xuất value giữa dấu ngoặc kép
                int startIndex = fullMessage.indexOf("'");
                if (startIndex >= 0) {
                    startIndex += 1; // Bỏ qua dấu mở ngoặc kép
                    int endIndex = fullMessage.indexOf("'", startIndex);
                    if (endIndex > startIndex) {
                        duplicateValue = fullMessage.substring(startIndex, endIndex);
                    }
                }
                
                // Thử trích xuất tên field từ constraint name hoặc message error
                String lowerMessage = fullMessage.toLowerCase();
                
                // Kiểm tra pattern của constraint name (MySQL auto-generated constraint names)
                // Định dạng: "Duplicate entry 'value' for key 'table.UK...'"
                if (lowerMessage.contains("ukdu5v5sr43g5bfnji4vb8hg5s3")) {
                    // Đây là constraint username (từ log error của bạn)
                    duplicateField = "username";
                } else if (lowerMessage.contains("username") || lowerMessage.contains("uk_username")) {
                    duplicateField = "username";
                } else if (lowerMessage.contains("email") || lowerMessage.contains("uk_email")) {
                    duplicateField = "email";
                } else if (lowerMessage.contains("phone") || lowerMessage.contains("uk_phone")) {
                    duplicateField = "phone";
                } else {
                    // Nếu không xác định được, kiểm tra pattern của value
                    // Phone thường có digits, email có @, username là alphanumeric
                    if (duplicateValue != null) {
                        if (duplicateValue.contains("@")) {
                            duplicateField = "email";
                        } else if (duplicateValue.matches("^[0-9]+$") && duplicateValue.length() >= 10) {
                            duplicateField = "phone";
                        } else {
                            duplicateField = "username";
                        }
                    }
                }
            }
        }
        
        List<Object> conflictItems = new ArrayList<>();
        Map<String, Object> conflictItem = new HashMap<>();
        conflictItem.put("field", duplicateField);
        conflictItem.put("value", duplicateValue);
        conflictItem.put("message", fullMessage != null ? fullMessage : "Duplicate entry");
        conflictItems.add(conflictItem);
        
        errors.put("conflictItems", conflictItems);
        errors.put("detailMessage", e.getMessage());
        if (duplicateField != null) {
            errors.put("duplicateField", duplicateField);
        }
        if (duplicateValue != null) {
            errors.put("duplicateValue", duplicateValue);
        }

        Response<?> response = new Response<>(
            409,
            messageSource.getMessage("response.error.conflictError", null, locale),
            "ExceptionHandle",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Validation Exception Handler (cho validation @RequestBody và method parameter)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    ResponseEntity<Response<?>> handleValidation(Exception e) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> errors = new LinkedHashMap<>();

        if (e instanceof MethodArgumentNotValidException) {
            // Xử lý validation cho @RequestBody @Valid
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            ex.getBindingResult().getFieldErrors().forEach(error -> {
                String key = error.getDefaultMessage();
                if(key != null && key.startsWith("{") && key.endsWith("}")){
                    key = key.substring(1, key.length() - 1);
                }
                String msg = messageSource.getMessage(
                    key != null ? key : error.getField(), null, key != null ? key : error.getField(), locale
                );
                String fieldName = error.getField();
                if(fieldName.contains(".")){
                    fieldName = fieldName.substring(fieldName.lastIndexOf(".") + 1);
                }
                errors.put(fieldName, msg);
            });
        } else if (e instanceof HandlerMethodValidationException) {
            // Xử lý validation cho method parameter (Spring Boot 3.x)
            HandlerMethodValidationException ex = (HandlerMethodValidationException) e;
            ex.getAllValidationResults().forEach(result -> {
                String parameterName = result.getMethodParameter().getParameterName();
                if (parameterName != null) {
                    result.getResolvableErrors().forEach(error -> {
                        String key = error.getDefaultMessage();
                        if (key != null && key.startsWith("{") && key.endsWith("}")) {
                            key = key.substring(1, key.length() - 1);
                        }
                        String msg = messageSource.getMessage(
                            key != null ? key : parameterName, null, key != null ? key : parameterName, locale
                        );
                        errors.put(parameterName, msg);
                    });
                }
            });
        }

        Response<?> response = new Response<>(
            400,
            messageSource.getMessage("response.error.validateFailed", null, locale),
            "ExceptionHandle",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Custom Validation Exception Handler (cho validation business logic)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationExceptionHandle.class)
    ResponseEntity<Response<?>> validationExceptionHandle(ValidationExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> errors = new LinkedHashMap<>();

        // Định dạng: field -> message (đồng nhất với các handler validation khác nhau)
        if(e.getInvalidFields() != null && !e.getInvalidFields().isEmpty()){
            e.getInvalidFields().forEach(field -> {
                if (field instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldMap = (Map<String, Object>) field;
                    String fieldName = fieldMap.get("field") != null ? fieldMap.get("field").toString() : "unknown";
                    String message = fieldMap.get("message") != null ? fieldMap.get("message").toString() : e.getMessage();
                    errors.put(fieldName, message);
                } else {
                    errors.put("error", field.toString());
                }
            });
        } else {
            errors.put("error", e.getMessage());
        }

        Response<?> response = new Response<>(
            400,
            messageSource.getMessage("response.error.validateFailed", null, locale),
            e.getModelName() != null ? e.getModelName() : "ExceptionHandle",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Invalid Format Exception Handler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<?>> handleInvalidFormat(HttpMessageNotReadableException ex) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof InvalidFormatException || cause instanceof MismatchedInputException) {
            List<JsonMappingException.Reference> path = ((JsonMappingException) cause).getPath();
            if (!path.isEmpty()) {
                String field = path.get(path.size() - 1).getFieldName();
                
                String key = String.format("validate.user.%s.invalidType", field);
                String msg = messageSource.getMessage(key, null, locale);
                
                if (msg.equals(key)) {
                    key = String.format("validate.%s.invalidType", field);
                    msg = messageSource.getMessage(key, null, locale);
                }
                
                if (msg.equals(key)) {
                    msg = locale.getLanguage().equals("vi") 
                            ? field + " không đúng định dạng." 
                            : field + " is invalid format.";
                }
                
                errors.put(field, msg);
            }
        } else {
            String errorMsg = messageSource.getMessage("response.error.validateFailed", null, locale);
            errors.put("error", errorMsg);
        }
        Response<?> response = new Response<>(
            400,
            messageSource.getMessage("response.error.validateFailed", null, locale),
            "ExceptionHandle",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Unauthorized Exception Handler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedExceptionHandle.class)
    ResponseEntity<Response<?>> unauthorizedExceptionHandle(UnauthorizedExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put("Error", e.getMessage());

        Response<?> response = new Response<>(
            401,
            messageSource.getMessage("response.error.unauthorizedError", null, locale),
            e.getModelName(),
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Forbidden Exception Handler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenExceptionHandle.class)
    ResponseEntity<Response<?>> forbiddenExceptionHandle(ForbiddenExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put("Error", e.getMessage());
        if(e.getRequiredRole() != null){
            errors.put("RequiredRole", e.getRequiredRole());
        }

        Response<?> response = new Response<>(
            403,
            messageSource.getMessage("response.error.forbiddenError", null, locale),
            e.getModelName(),
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Access Denied Exception Handler (Spring Security - khi user không có quyền truy cập)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Response<?>> accessDeniedExceptionHandle(AccessDeniedException e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put(
            "Error", e.getMessage() + " :You are not authorized to access this resource"
        );

        Response<?> response = new Response<>(
            403,
            messageSource.getMessage("response.error.forbiddenError", null, locale),
            "ExceptionHandle",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Service Unavailable Exception Handler
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ServiceUnavailableExceptionHandle.class)
    ResponseEntity<Response<?>> serviceUnavailableExceptionHandle(ServiceUnavailableExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> errors = new HashMap<>();
        errors.put("Error", e.getMessage());

        Response<?> response = new Response<>(
            503,
            messageSource.getMessage("response.error.serviceUnavailableError", null, locale),
            e.getServiceName() != null ? e.getServiceName() : "Service",
            errors,
            null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Too Many Requests Exception Handler
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(TooManyRequestsExceptionHandle.class)
    ResponseEntity<Response<?>> tooManyRequestsExceptionHandler(TooManyRequestsExceptionHandle e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> error = new HashMap<>();
        error.put("Error", e.getMessage());
        
        Response<?> response = new Response<>(
                429,
                messageSource.getMessage("response.error.tooManyRequests", null, locale),
                e.getControllerName() != null ? e.getControllerName() : "RateLimit",
                error,
                null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Exception Handler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    ResponseEntity<Response<?>> exceptionHandler(Exception e) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> error = new HashMap<>();
        error.put("Error", e.getMessage());
        
        Response<?> response = new Response<>(
                500,
                messageSource.getMessage("response.error.internalServerError", null, locale),
                "ExceptionHandle",
                error,
                null
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

}
