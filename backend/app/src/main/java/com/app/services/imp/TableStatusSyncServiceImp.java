package com.app.services.imp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.app.services.TableStatusSyncService;
import com.app.utils.OrderTableHoldUtils;
import com.common.entities.TableEntity;
import com.common.enums.TableStatus;
import com.common.utils.ReservationHoldUtils;
import com.common.repositories.OrderRepository;
import com.common.repositories.ReservationRepository;
import com.common.repositories.TableRepository;
import com.common.utils.FilterPageCacheFacade;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Service
public class TableStatusSyncServiceImp implements TableStatusSyncService {

    private static final String TABLE_REDIS_KEY_PREFIX = "table:";

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private TableRepository tableRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, Integer tableNumber) {
        return LogContext.builder()
            .module("app")
            .className(getClass().getSimpleName())
            .methodName(methodName)
            .ids(Collections.singletonList(tableNumber))
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void syncTableStatus(Integer tableNumber) {
        syncTableStatus(tableNumber, null);
    }

    // đồng bộ trạng thái cho bàn cho reservation service
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void syncTableStatus(Integer tableNumber, Integer excludeReservationId) {
        LogContext logContext = getLogContext("syncTableStatus", tableNumber);

        if (tableNumber == null) {
            return;
        }
        // tìm bàn theo table number
        tableRepository.findByTableNumber(tableNumber).ifPresent(table -> {
            // xác định trạng thái mới cho bàn
            TableStatus target = resolveTargetStatus(tableNumber, excludeReservationId);
            // nếu trạng thái hiện tại giống với trạng thái mới thì không cần update
            if (table.getTableStatus() == target) {
                return;
            }

            table.setTableStatus(target);
            tableRepository.save(table);

            FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);

            log.logInfo(
                "Table " + tableNumber + " synced to " + target + " (orders/reservations)",
                logContext
            );
        });
    }

    // đồng bộ trạng thái cho tất cả bàn
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void reconcileAllTableStatuses() {
        LogContext logContext = getLogContext("reconcileAllTableStatuses", null);

        Set<Integer> tableNumbers = new HashSet<>();

        // tìm tất cả bàn có trạng thái OCCUPIED
        tableRepository.findByTableStatus(TableStatus.OCCUPIED).stream()
            .map(TableEntity::getTableNumber)
            .forEach(tableNumbers::add);

        // tìm tất cả bàn có trạng thái RESERVED
        tableRepository.findByTableStatus(TableStatus.RESERVED).stream()
            .map(TableEntity::getTableNumber)
            .forEach(tableNumbers::add);

        if (tableNumbers.isEmpty()) {
            return;
        }

        // đồng bộ trạng thái cho tất cả bàn
        tableNumbers.forEach(this::syncTableStatus);
        log.logInfo("Reconciled table status for " + tableNumbers.size() + " table(s)", logContext);
    }

    // xác định trạng thái mới cho bàn
    private TableStatus resolveTargetStatus(Integer tableNumber, Integer excludeReservationId) {
        // kiểm tra xem có đơn giữ bàn nào đang giữ bàn không
        // nếu có thì trạng thái sẽ là OCCUPIED
        if (orderRepository.existsActiveHoldingOrderOnTable(
            tableNumber, OrderTableHoldUtils.TABLE_HOLDING_ORDER_STATUSES, null
        )) {
            return TableStatus.OCCUPIED;
        }
        // kiểm tra xem có reservation nào đang giữ bàn này không
        // nếu có thì trạng thái sẽ là RESERVED
        if (hasActiveReservationOnTable(tableNumber, excludeReservationId)) {
            return TableStatus.RESERVED;
        }
        // nếu không có thì trạng thái sẽ là AVAILABLE
        return TableStatus.AVAILABLE;
    }

    // dùng cho reservation service
    // kiểm tra xem có reservation nào đang giữ bàn này không
    private boolean hasActiveReservationOnTable(Integer tableNumber, Integer excludeReservationId) {
        return reservationRepository.existsActiveReservationOnTable(
            tableNumber,
            ReservationHoldUtils.ACTIVE_HOLD_STATUSES,
            excludeReservationId
        );
    }

}
