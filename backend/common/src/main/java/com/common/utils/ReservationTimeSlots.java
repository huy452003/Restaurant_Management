package com.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import com.common.enums.ReservationSlot;

public final class ReservationTimeSlots {

    public static final LocalTime OPEN = ReservationSlot.S10_00.toLocalTime();
    public static final LocalTime CLOSE = LocalTime.of(22, 0);
    public static final int STEP_MINUTES = 30;

    /** Cửa sổ đặt gần giờ hiện tại — bắt buộc kiểm tra trạng thái bàn vật lý. */
    public static final long NEAR_WINDOW_MINUTES = 30L;

    private ReservationTimeSlots() {
    }

    public static boolean isNearWindow(LocalDate date, LocalTime time) {
        LocalDateTime schedule = toDateTime(date, time);
        return !schedule.isAfter(LocalDateTime.now().plusMinutes(NEAR_WINDOW_MINUTES));
    }

    public static List<LocalTime> all() {
        return ReservationSlot.all().stream()
            .map(ReservationSlot::toLocalTime)
            .collect(Collectors.toList());
    }

    public static boolean isValidSlot(LocalTime time) {
        return ReservationSlot.isValid(time);
    }

    public static LocalDateTime toDateTime(LocalDate date, LocalTime time) {
        return ReservationSlot.toDateTime(date, time);
    }

    public static String format(LocalTime time) {
        return ReservationSlot.from(time).map(ReservationSlot::label).orElse(time.toString());
    }

    public static boolean isPastSlot(LocalDate date, LocalTime time, LocalDateTime now) {
        return ReservationSlot.isPastSlot(date, time, now);
    }
}
