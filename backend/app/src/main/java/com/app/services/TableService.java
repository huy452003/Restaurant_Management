package com.app.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.TableStatus;
import com.common.models.table.TableModel;
import com.common.models.table.TableRequestModel;

public interface TableService {
    Page<TableModel> filters(
        Integer id, Integer tableNumber, Integer capacity,
        TableStatus tableStatus, String location, boolean excludeTablesWithPendingOrder,
        boolean freshSnapshot,
        Pageable pageable
    );
    List<TableModel> create(List<TableRequestModel> tables);
    List<TableModel> update(List<TableRequestModel> updates, List<Integer> tableIds);

    /** Trả bàn theo đơn/reservation thực tế; báo lỗi nếu vẫn còn đơn giữ bàn. */
    TableModel markAvailable(Integer tableId);
}
