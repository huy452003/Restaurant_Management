package com.common.enums;

public enum OrderStatus {
    PENDING, // chờ xác nhận
    CONFIRMED, // đã xác nhận
    PREPARING, // đang chuẩn bị (sau khi thanh toán đủ ở CONFIRMED)
    COMPLETED, // đã phục vụ xong (quầy xác nhận)
    CANCELLED; // đã hủy
}
