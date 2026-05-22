package com.app.utils;

import com.common.repositories.OrderRepository;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

public final class TableHoldGuard {

    private TableHoldGuard() {
    }

    public static void assertNoHoldingOrderOnTable(
        OrderRepository orderRepository,
        Integer tableNumber,
        Integer excludeOrderId,
        String modelName,
        LogContext logContext,
        LoggingService log
    ) {
        if (!orderRepository.existsActiveHoldingOrderOnTable(
            tableNumber,
            OrderTableHoldUtils.TABLE_HOLDING_ORDER_STATUSES,
            excludeOrderId
        )) {
            return;
        }
        ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
            "Table " + tableNumber
                + " already has an active order. Choose another table or try again later.",
            modelName,
            "table has active holding order"
        );
        log.logError(e.getMessage(), e, logContext);
        throw e;
    }
}
