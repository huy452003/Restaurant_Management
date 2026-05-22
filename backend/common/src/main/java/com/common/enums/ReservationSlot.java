package com.common.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ReservationSlot {
    S10_00(10, 0),
    S10_30(10, 30),
    S11_00(11, 0),
    S11_30(11, 30),
    S12_00(12, 0),
    S12_30(12, 30),
    S13_00(13, 0),
    S13_30(13, 30),
    S14_00(14, 0),
    S14_30(14, 30),
    S15_00(15, 0),
    S15_30(15, 30),
    S16_00(16, 0),
    S16_30(16, 30),
    S17_00(17, 0),
    S17_30(17, 30),
    S18_00(18, 0),
    S18_30(18, 30),
    S19_00(19, 0),
    S19_30(19, 30),
    S20_00(20, 0),
    S20_30(20, 30),
    S21_00(21, 0),
    S21_30(21, 30);

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalTime time;

    ReservationSlot(int hour, int minute) {
        this.time = LocalTime.of(hour, minute);
    }

    public LocalTime toLocalTime() {
        return time;
    }

    public String label() {
        return time.format(LABEL);
    }

    public static List<ReservationSlot> all() {
        return List.of(values());
    }

    public static boolean isValid(LocalTime value) {
        return from(value).isPresent();
    }

    public static Optional<ReservationSlot> from(LocalTime value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(s -> s.time.equals(value))
            .findFirst();
    }

    public static LocalDateTime toDateTime(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time);
    }

    public static boolean isPastSlot(LocalDate date, LocalTime time, LocalDateTime now) {
        return toDateTime(date, time).isBefore(now);
    }
}
