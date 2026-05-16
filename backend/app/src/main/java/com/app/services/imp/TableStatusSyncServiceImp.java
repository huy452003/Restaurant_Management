package com.app.services.imp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.app.services.TableStatusSyncService;
import com.app.utils.OrderTableHoldUtils;
import com.common.entities.TableEntity;
import com.common.enums.OrderStatus;
import com.common.enums.ReservationStatus;
import com.common.enums.TableStatus;
import com.common.repositories.OrderRepository;
import com.common.repositories.ReservationRepository;
import com.common.repositories.TableRepository;
import com.common.utils.FilterPageCacheFacade;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.time.LocalDateTime;

@Service
public class TableStatusSyncServiceImp implements TableStatusSyncService {

    private static final String TABLE_REDIS_KEY_PREFIX = "table:";
    private static final long NEAR_RESERVATION_WINDOW_MINUTES = 30L;

    private static final List<ReservationStatus> NEAR_ACTIVE_RESERVATION_STATUSES = Arrays.asList(
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        ReservationStatus.SEATED
    );

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
        if (orderRepository.existsByTable_TableNumberAndOrderStatusIn(
            tableNumber, OrderTableHoldUtils.TABLE_HOLDING_ORDER_STATUSES
        )) {
            return TableStatus.OCCUPIED;
        }
        if (hasNearActiveReservation(tableNumber, excludeReservationId)) {
            return TableStatus.RESERVED;
        }
        return TableStatus.AVAILABLE;
    }

    private boolean hasNearActiveReservation(Integer tableNumber, Integer excludeReservationId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(NEAR_RESERVATION_WINDOW_MINUTES);
        LocalDateTime windowEnd = now.plusMinutes(NEAR_RESERVATION_WINDOW_MINUTES);
        return reservationRepository.existsActiveReservationInWindow(
            tableNumber,
            windowStart,
            windowEnd,
            NEAR_ACTIVE_RESERVATION_STATUSES,
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
