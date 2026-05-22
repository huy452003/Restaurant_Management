package com.app.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.ReservationStatus;
import com.common.models.reservation.ReservationAdminRequestModel;
import com.common.models.reservation.ReservationAvailabilityModel;
import com.common.models.reservation.ReservationCustomerCreateModel;
import com.common.models.reservation.ReservationCustomerUpdateModel;
import com.common.models.reservation.ReservationModel;

public interface ReservationService {

    Page<ReservationModel> filtersForCustomer(
        Integer id, Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus, Pageable pageable
    );

    Page<ReservationModel> filtersForAdmin(
        Integer id, String customerName, String customerPhone, String customerEmail,
        Integer tableNumber, LocalDate reservationDate, LocalTime reservationTime,
        Integer numberOfGuests, ReservationStatus reservationStatus, Pageable pageable
    );

    List<ReservationModel> create(List<ReservationCustomerCreateModel> reservations);
    ReservationModel updateForCustomer(ReservationCustomerUpdateModel update, Integer reservationId);
    List<ReservationModel> updateByAdmin(List<ReservationAdminRequestModel> updates, List<Integer> reservationIds);
    ReservationModel cancel(Integer reservationId);
    ReservationAvailabilityModel getTimeSlotAvailability(Integer tableNumber, LocalDate date);
}

