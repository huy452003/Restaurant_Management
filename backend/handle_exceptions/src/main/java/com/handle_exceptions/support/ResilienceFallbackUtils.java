package com.handle_exceptions.support;

import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ServiceUnavailableExceptionHandle;
import com.handle_exceptions.TooManyRequestsExceptionHandle;
import com.handle_exceptions.UnauthorizedExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import java.util.function.Predicate;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Chuẩn fallback Resilience4j: không trả fake data — re-throw lỗi nghiệp vụ,
 * 503 khi circuit OPEN, 429 khi rate limit, còn lại propagate lỗi infra.
 */
public final class ResilienceFallbackUtils {

    private ResilienceFallbackUtils() {
    }

    // tìm lỗi nghiệp vụ từ chain
    private static RuntimeException findBusinessRuntimeException(Throwable throwable) {
        Throwable found = findInCauseChain(throwable, t ->
            t instanceof ConflictExceptionHandle
                || t instanceof NotFoundExceptionHandle
                || t instanceof ValidationExceptionHandle
                || t instanceof ForbiddenExceptionHandle
                || t instanceof UnauthorizedExceptionHandle
                || t instanceof BadCredentialsException
        );
        return found instanceof RuntimeException runtime ? runtime : null;
    }

    private static <T extends Throwable> T findInCauseChain(Throwable throwable, Class<T> type) {
        Throwable found = findInCauseChain(throwable, type::isInstance);
        return type.isInstance(found) ? type.cast(found) : null;
    }

    private static Throwable findInCauseChain(Throwable throwable, Predicate<Throwable> matcher) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (matcher.test(cause)) {
                return cause;
            }
        }
        return null;
    }

    // kiểm tra lỗi nghiệp vụ
    public static boolean isBusinessThrowable(Throwable throwable) {
        return findBusinessRuntimeException(throwable) != null;
    }

    // kiểm tra lỗi circuit breaker open
    public static boolean isCircuitBreakerOpen(Throwable throwable) {
        return findInCauseChain(throwable, CallNotPermittedException.class) != null;
    }

    // kiểm tra lỗi rate limit exceeded
    public static boolean isRateLimitExceeded(Throwable throwable) {
        return findInCauseChain(throwable, RequestNotPermitted.class) != null;
    }

    // re-throw lỗi nghiệp vụ từ chain, không làm gì nếu không phải business
    public static void rethrowBusinessThrowable(Throwable throwable) {
        RuntimeException business = findBusinessRuntimeException(throwable);
        if (business != null) {
            throw business;
        }
    }

    // xử lý lỗi service unavailable khi circuit breaker open
    public static ServiceUnavailableExceptionHandle serviceUnavailable(
        String operation, Throwable throwable
    ) {
        String message = throwable != null && throwable.getMessage() != null
            ? throwable.getMessage()
            : "Circuit breaker open for " + operation;
        return new ServiceUnavailableExceptionHandle(message, operation);
    }

    // throw lỗi runtime
    public static void throwAsRuntime(Throwable throwable) {
        if (throwable == null) {
            throw new RuntimeException("Unknown failure");
        }
        if (throwable instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (throwable instanceof Exception exception) {
            throw new RuntimeException(exception);
        }
        throw new RuntimeException(throwable);
    }

    /**
     * Fallback CircuitBreaker chuẩn (không degraded): business → infra → 503 nếu CB OPEN.
     */
    public static void propagateCircuitBreakerFailure(Throwable throwable, String operation) {
        // re-throw lỗi nghiệp vụ từ chain
        rethrowBusinessThrowable(throwable);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!isCircuitBreakerOpen(throwable)) {
            throwAsRuntime(throwable);
        }
        // lỗi circuit breaker open -> throw service unavailable exception
        throw serviceUnavailable(operation, throwable);
    }

    /**
     * Fallback RateLimiter trên controller: business → infra → 429 nếu bị limit.
     */
    public static void propagateRateLimitFailure(Throwable throwable, String endpoint) {
        // re-throw lỗi nghiệp vụ từ chain
        rethrowBusinessThrowable(throwable);
        // nếu không phải lỗi rate limit exceeded -> throw runtime exception
        if (!isRateLimitExceeded(throwable)) {
            throwAsRuntime(throwable);
        }
        // lỗi rate limit exceeded -> throw too many requests exception
        String message = throwable != null && throwable.getMessage() != null
            ? throwable.getMessage()
            : "Rate limit exceeded for " + endpoint;
        throw new TooManyRequestsExceptionHandle(message, endpoint);
    }
}
