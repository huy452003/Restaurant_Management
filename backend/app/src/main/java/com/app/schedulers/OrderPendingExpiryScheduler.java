package com.app.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.services.OrderService;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Component
public class OrderPendingExpiryScheduler {
    @Autowired
    private OrderService orderService;
    @Autowired
    private LoggingService log;

    @Scheduled(fixedRateString = "${order.pending.expiry-check-interval-ms:60000}")
    public void expireStalePendingOrders() {
        LogContext logContext = LogContext.builder()
            .module("app")
            .className(getClass().getSimpleName())
            .methodName("expireStalePendingOrders")
            .build();
        int expired = orderService.expireStalePendingOrders();
        if (expired > 0) {
            log.logInfo("Auto-cancelled " + expired + " stale PENDING order(s)", logContext);
        }
    }
}
