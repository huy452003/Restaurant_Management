package com.app.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.ShiftStatus;
import com.common.models.user.ShiftModel;

public interface ShiftService {
    Page<ShiftModel> filters(
        Integer id, LocalDate shiftDate, 
        LocalDateTime startTime, LocalDateTime endTime,
        ShiftStatus shiftStatus, Pageable pageable
    );
    List<ShiftModel> create(List<ShiftModel> shifts);
    List<ShiftModel> update(List<ShiftModel> updates, List<Integer> shiftIds);
}
