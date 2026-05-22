package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

import com.common.enums.ReservationStatus;

@Entity
@Table(name = "reservations")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor 
public class ReservationEntity extends BaseEntity {
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;
    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;
    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;
    @Column(name = "reservation_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;
    @Column(name = "special_request")
    private String specialRequest;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "table_number", referencedColumnName = "table_number")
    private TableEntity table;
}
