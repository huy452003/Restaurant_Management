package com.app.services.imp;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    @Override
    public void syncTableStatus(Integer tableNumber) {
        syncTableStatus(tableNumber, null);
    }

    @Override
    public void syncTableStatus(Integer tableNumber, Integer excludeReservationId) {
        if (tableNumber == null) {
            return;
        }
        tableRepository.findByTableNumber(tableNumber).ifPresent(table -> {
            TableStatus target = resolveTargetStatus(tableNumber, excludeReservationId);
            if (table.getTableStatus() == target) {
                return;
            }
            table.setTableStatus(target);
            tableRepository.save(table);
            FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
            LogContext logContext = LogContext.builder()
                .module("app")
                .className(getClass().getSimpleName())
                .methodName("syncTableStatus")
                .build();
            log.logInfo(
                "Table " + tableNumber + " synced to " + target + " (orders/reservations)",
                logContext
            );
        });
    }

    private TableStatus resolveTargetStatus(Integer tableNumber, Integer excludeReservationId) {
        if (orderRepository.existsActiveHoldingOrderOnTable(
            tableNumber, OrderTableHoldUtils.TABLE_HOLDING_ORDER_STATUSES, null
        )) {
            return TableStatus.OCCUPIED;
        }
        if (hasActiveReservationOnTable(tableNumber, excludeReservationId)) {
            return TableStatus.RESERVED;
        }
        return TableStatus.AVAILABLE;
    }

    /** PENDING/CONFIRMED trên bàn → RESERVED (khóa bàn cho vận hành, không chỉ cửa sổ ±30 phút). */
    private boolean hasActiveReservationOnTable(Integer tableNumber, Integer excludeReservationId) {
        return reservationRepository.existsActiveReservationOnTable(
            tableNumber,
            ReservationHoldUtils.ACTIVE_HOLD_STATUSES,
            excludeReservationId
        );
    }

    @Override
    public void reconcileAllTableStatuses() {
        LogContext logContext = LogContext.builder()
            .module("app")
            .className(getClass().getSimpleName())
            .methodName("reconcileAllTableStatuses")
            .build();
        Set<Integer> tableNumbers = new HashSet<>();
        tableRepository.findByTableStatus(TableStatus.OCCUPIED).stream()
            .map(TableEntity::getTableNumber)
            .forEach(tableNumbers::add);
        tableRepository.findByTableStatus(TableStatus.RESERVED).stream()
            .map(TableEntity::getTableNumber)
            .forEach(tableNumbers::add);
        if (tableNumbers.isEmpty()) {
            return;
        }
        tableNumbers.forEach(this::syncTableStatus);
        log.logInfo("Reconciled table status for " + tableNumbers.size() + " table(s)", logContext);
    }
}
