package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.TableStatus;
import java.util.List;

@Entity
@Table(name = "tables")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class TableEntity extends BaseEntity {
    @Column(name = "table_number", nullable = false, unique = true)
    private Integer tableNumber;
    @Column(name = "capacity")
    private Integer capacity;
    @Column(name = "table_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TableStatus tableStatus;
    @Column(name = "location", nullable = false)
    private String location;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY)
    private List<ReservationEntity> reservations;
    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY)
    private List<OrderEntity> orders;
}
