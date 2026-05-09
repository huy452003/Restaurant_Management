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

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.LocaleResolver;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import org.springframework.core.annotation.Order;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

@RestControllerAdvice
@Order(1) // Độ ưu tiên cao để catch DataIntegrityViolationException trước handler Exception tổng quát
public class CustomExceptionHandler {
    @Autowired
    MessageSource messageSource;
    @Autowired
    private LocaleResolver localeResolver;

    /** Giống DispatcherServlet: luôn đọc Accept-Language qua LocaleResolver, tránh rơi về Locale.getDefault() của JVM. */
    private Locale resolveLocale(HttpServletRequest request) {
        if (request != null && localeResolver != null) {
            Locale l = localeResolver.resolveLocale(request);
            return l != null ? l : Locale.ENGLISH;
        }
        return Locale.ENGLISH;
    }

    // Not Found Exception Handler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundExceptionHandle.class)
    ResponseEntity<Response<?>> notFoundExceptionHandle(NotFoundExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> conflictExceptionHandle(ConflictExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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

    private static final Pattern MYSQL_FK_COLUMN = Pattern.compile(
        "FOREIGN KEY\\s*\\(\\s*`([^`]+)`\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    /** FK / tham chiếu không hợp lệ ≠ trùng unique — HTTP 409 chỉ giữ cho duplicate/conflict có chủ đích. */
    private static boolean isForeignKeyOrReferenceViolation(CharSequence msg) {
        if (msg == null || msg.length() == 0) {
            return false;
        }
        String m = msg.toString().toLowerCase();
        return m.contains("foreign key constraint")
            || m.contains("a foreign key constraint fails")
            || m.contains("cannot add or update a child row")
            || m.contains("cannot delete or update a parent row")
            || m.contains("violates foreign key constraint");
    }

    private static String sqlColumnHintFromForeignKeyMessage(String fullMessage) {
        if (!StringUtils.hasText(fullMessage)) {
            return null;
        }
        Matcher matcher = MYSQL_FK_COLUMN.matcher(fullMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /** Chuyển category_name → categoryName để khớp tên field trên JSON API. */
    private static String sqlColumnToApiField(String sqlColumn) {
        if (!StringUtils.hasText(sqlColumn)) {
            return null;
        }
        String[] parts = sqlColumn.trim().split("_");
        StringBuilder camel = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            camel.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                camel.append(parts[i], 1, parts[i].length());
            }
        }
        return camel.toString();
    }

    // Data Integrity Violation: duplicate unique → 409; foreign key / invalid reference → 400
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Response<?>> dataIntegrityViolationExceptionHandle(DataIntegrityViolationException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        Map<String, Object> errors = new HashMap<>();
        String errorMessage = e.getMessage();
        String rootCauseMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : null;
        String fullMessage = rootCauseMessage != null ? rootCauseMessage : errorMessage;

        if (isForeignKeyOrReferenceViolation(fullMessage)) {
            String sqlColumn = sqlColumnHintFromForeignKeyMessage(fullMessage);
            String apiField = sqlColumnToApiField(sqlColumn);
            errors.put(
                "detailMessage",
                fullMessage != null ? fullMessage : e.getMessage()
            );
            List<Map<String, Object>> invalidRefs = new ArrayList<>();
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("field", apiField != null ? apiField : "reference");
            ref.put(
                "message",
                messageSource.getMessage("response.error.foreignKeyViolation.detail", null, locale)
            );
            invalidRefs.add(ref);
            errors.put("invalidReferences", invalidRefs);

            Response<?> fkResponse = new Response<>(
                400,
                messageSource.getMessage("response.error.foreignKeyViolation", null, locale),
                "ExceptionHandle",
                errors,
                null
            );
            return ResponseEntity.badRequest().body(fkResponse);
        }

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
    ResponseEntity<Response<?>> handleValidation(Exception e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
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

    // ConstraintViolation Exception Handler (cho validation ở method-level như @Validated)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Response<?>> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        Map<String, Object> errors = new LinkedHashMap<>();

        e.getConstraintViolations().forEach(violation -> {
            String fieldPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "field";
            String key = violation.getMessage();
            if (key != null && key.startsWith("{") && key.endsWith("}")) {
                key = key.substring(1, key.length() - 1);
            }
            String msg = messageSource.getMessage(
                key != null ? key : fieldPath,
                null,
                key != null ? key : fieldPath,
                locale
            );
            errors.put(fieldPath, msg);
        });

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
    ResponseEntity<Response<?>> validationExceptionHandle(ValidationExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("detailMessage", e.getMessage());

        List<Object> nonMapRefs = new ArrayList<>();
        // Định dạng: field -> message (đồng nhất với các handler validation khác nhau)
        if(e.getInvalidFields() != null && !e.getInvalidFields().isEmpty()){
            for(Object field : e.getInvalidFields()) {
                if (field instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldMap = (Map<String, Object>) field;
                    String fieldName = fieldMap.get("field") != null ? fieldMap.get("field").toString() : "unknown";
                    String message = fieldMap.get("message") != null ? fieldMap.get("message").toString() : e.getMessage();
                    errors.put(fieldName, message);
                } else {
                    nonMapRefs.add(field);
                }
            }
            if(!nonMapRefs.isEmpty()) {
                errors.put("invalidFieldRefs", nonMapRefs);
            }
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
    public ResponseEntity<Response<?>> handleInvalidFormat(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> unauthorizedExceptionHandle(UnauthorizedExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> forbiddenExceptionHandle(ForbiddenExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

        Map<String, Object> errors = new HashMap<>();
        errors.put("Error", e.getMessage());
        if(e.getRequired() != null){
            errors.put("Required", e.getRequired());
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
    ResponseEntity<Response<?>> accessDeniedExceptionHandle(AccessDeniedException e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> serviceUnavailableExceptionHandle(ServiceUnavailableExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> tooManyRequestsExceptionHandler(TooManyRequestsExceptionHandle e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
    ResponseEntity<Response<?>> exceptionHandler(Exception e, HttpServletRequest request) {
        Locale locale = resolveLocale(request);

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
