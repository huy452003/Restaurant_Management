package com.app.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.services.OrderService;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Component
public class OrderConfirmedUnpaidExpiryScheduler {
    @Autowired
    private OrderService orderService;
    @Autowired
    private LoggingService log;

    @Scheduled(fixedRateString = "${order.confirmed.unpaid.expiry-check-interval-ms:60000}")
    public void expireStaleConfirmedUnpaidOrders() {
        LogContext logContext = LogContext.builder()
            .module("app")
            .className(getClass().getSimpleName())
            .methodName("expireStaleConfirmedUnpaidOrders")
            .build();
        int affected = orderService.expireStaleConfirmedUnpaidOrders();
        if (affected > 0) {
            log.logInfo(
                "Auto-cancelled " + affected + " stale CONFIRMED unpaid order(s) (DINE_IN + DELIVERY)",
                logContext
            );
        }
    }
}
