package com.app.utils;

import com.common.repositories.OrderRepository;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

public final class TableHoldGuard {

    private TableHoldGuard() {
    }

    // kiểm tra xem bàn có đơn giữ bàn không
    public static void assertNoHoldingOrderOnTable(
        OrderRepository orderRepository, Integer tableNumber,
        Integer excludeOrderId, String modelName, LogContext logContext,
        LoggingService log
    ) {
        // nếu không có đơn nào đang sử dụng bàn này thì không cần kiểm tra
        if (!orderRepository.existsActiveHoldingOrderOnTable(
            tableNumber,
            OrderTableHoldUtils.TABLE_HOLDING_ORDER_STATUSES,
            excludeOrderId
        )) {
            return;
        }
        // nếu có đơn nào đang sử dụng bàn này thì không được phép assign bàn
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
