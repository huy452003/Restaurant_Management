package com.common.enums;

public enum OrderItemStatus {
    PENDING, // chờ xác nhận
    CONFIRMED, // đã xác nhận
    PREPARING, // đang chuẩn bị
    READY, // đã sẵn sàng
    SERVED, // đã phục vụ
    COMPLETED, // đã hoàn thành
    CANCELLED; // đã hủy
}
