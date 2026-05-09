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
        TableStatus tableStatus, String location, Pageable pageable
    );
    List<TableModel> create(List<TableRequestModel> tables);
    List<TableModel> update(List<TableRequestModel> updates, List<Integer> tableIds);
    TableModel markReserved(Integer tableId);
    TableModel markOccupied(Integer tableId);
    TableModel markCleaning(Integer tableId);
    TableModel markAvailable(Integer tableId);
}
