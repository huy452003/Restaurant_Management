package com.app.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.ReservationStatus;
import com.common.models.reservation.ReservationAdminRequestModel;
import com.common.models.reservation.ReservationCustomerRequestModel;
import com.common.models.reservation.ReservationModel;

public interface ReservationService {
    Page<ReservationModel> filtersForCustomer(
        Integer id, Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus, Pageable pageable
    );
    Page<ReservationModel> filtersForAdmin(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDateTime reservationTs, Integer numberOfGuests,
        ReservationStatus reservationStatus, Pageable pageable
    );
    List<ReservationModel> create(List<ReservationCustomerRequestModel> reservations);
    ReservationModel updateForCustomer(ReservationCustomerRequestModel update, Integer reservationId);
    List<ReservationModel> updateByAdmin(List<ReservationAdminRequestModel> updates, List<Integer> reservationIds);
    ReservationModel cancel(Integer reservationId);
}
