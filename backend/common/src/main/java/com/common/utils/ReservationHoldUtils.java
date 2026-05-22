package com.common.utils;

import java.util.List;

import com.common.enums.ReservationStatus;

/** Trạng thái đặt bàn đang giữ slot / khóa bàn vận hành (khớp {@code TableStatusSyncServiceImp}). */
public final class ReservationHoldUtils {

    public static final List<ReservationStatus> ACTIVE_HOLD_STATUSES = List.of(
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED
    );

    private ReservationHoldUtils() {
    }

    public static boolean isTerminal(ReservationStatus status) {
        return status == ReservationStatus.CANCELLED
            || status == ReservationStatus.COMPLETED
            || status == ReservationStatus.NO_SHOW;
    }
}
