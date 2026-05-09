package com.app.services.imp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.app.services.TableService;
import com.common.repositories.TableRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.entities.TableEntity;
import com.common.enums.TableStatus;
import com.common.models.table.TableRequestModel;
import com.common.models.table.TableModel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import org.modelmapper.ModelMapper;

@Service
public class TableServiceImp implements TableService {
    @Autowired
    private TableRepository tableRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

    private LogContext getLogContext(String methodName, List<Integer> tableIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(tableIds)
            .build();
    }

    private static final String TABLE_REDIS_KEY_PREFIX = "table:";

    @Override
    public Page<TableModel> filters(
        Integer id, Integer tableNumber, Integer capacity, 
        TableStatus tableStatus, String location, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering tables with pagination ...!", logContext);

        List<FilterCondition<TableEntity>> conditions = buildFilterConditions(
            id, tableNumber, capacity, tableStatus, location
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            TABLE_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<TableModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, TableModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " tables in cache", logContext);
            return cached;
        }

        Page<TableEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = tableRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all tables with pagination", logContext);
        }else {
            Specification<TableEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = tableRepository.findAll(spec, pageable);
        }

        List<TableModel> pageDatas = pageEntities.getContent().stream().map(
            tableEntity -> modelMapper.map(tableEntity, TableModel.class)
        ).collect(Collectors.toList());

        Page<TableModel> tableModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, tableModelPage);
            log.logInfo("cached first-page filter snapshot for " + tableModelPage.getTotalElements()
                + " tables, key: " + redisKeyFilters, logContext);
        }
        return tableModelPage;
    }

    @Override
    public List<TableModel> create(List<TableRequestModel> tables) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating tables ...!", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(TableRequestModel table : tables) {
            if(tableRepository.existsByTableNumber(table.getTableNumber())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "tableNumber");
                conflict.put("value", table.getTableNumber());
                conflict.put("message", "Table number already exists");
                conflicts.add(conflict);
            }
        }

        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "TableModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<TableEntity> tableEntities = tables.stream().map(
            tableModel -> {
                TableEntity tableEntity = modelMapper.map(tableModel, TableEntity.class);
                tableEntity.setTableStatus(TableStatus.AVAILABLE);
                return tableEntity;
            } 
        ).collect(Collectors.toList());

        tableRepository.saveAll(tableEntities);

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);

        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created " + tableEntities.size() + " tables", logContext);
        return tableEntities.stream().map(
            entity -> modelMapper.map(entity, TableModel.class)
        ).collect(Collectors.toList());
    }
    
    @Override
    public List<TableModel> update(List<TableRequestModel> updates, List<Integer> tableIds) {
        LogContext logContext = getLogContext(
            "update", tableIds != null ? tableIds : Collections.emptyList()
        );
        log.logInfo("Updating tables ...!", logContext);

        if(updates.size() != tableIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and tableIds",
                tableIds,
                "TableModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<TableEntity> foundTables = tableIds.stream().map(
            id -> tableRepository.findById(id).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "Table not found with id: " + id,
                    Collections.singletonList(id),
                    "TableModel"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            })
        ).collect(Collectors.toList());
        log.logInfo("found " + foundTables.size() + " tables", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(int i = 0; i < updates.size(); i++) {
            TableRequestModel requestTable = updates.get(i);
            TableEntity currentTable = foundTables.get(i);
            if(!Objects.equals(requestTable.getTableNumber(), currentTable.getTableNumber())) {
                if(tableRepository.existsByTableNumber(requestTable.getTableNumber())) {
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "tableNumber");
                    conflict.put("value", requestTable.getTableNumber());
                    conflict.put("message", "Table number already exists");
                    conflicts.add(conflict);
                }
            }
        }   
        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "TableModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<TableEntity> tablesToUpdate = new ArrayList<>();
        Iterator<TableRequestModel> tableIterator = updates.iterator();
        Iterator<TableEntity> currentTableIterator = foundTables.iterator();

        while(tableIterator.hasNext() && currentTableIterator.hasNext()) {
            TableRequestModel update = tableIterator.next();
            TableEntity current = currentTableIterator.next();

            Boolean hasChanges = !Objects.equals(update.getTableNumber(), current.getTableNumber()) ||
                                 !Objects.equals(update.getCapacity(), current.getCapacity()) ||
                                 !Objects.equals(update.getLocation(), current.getLocation());
            if(hasChanges) {
                current.setTableNumber(update.getTableNumber());
                current.setCapacity(update.getCapacity());
                current.setLocation(update.getLocation());
                tablesToUpdate.add(current);
            }
        }

        if(!tablesToUpdate.isEmpty()) {
            tableRepository.saveAll(tablesToUpdate);
            log.logInfo("completed, updated " + tablesToUpdate.size() + " tables", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);

        return foundTables.stream().map(
            entity -> modelMapper.map(entity, TableModel.class)
        ).collect(Collectors.toList());
    }

    // đánh dấu bàn có sẵn
    @Override
    public TableModel markAvailable(Integer tableId) {
        LogContext logContext = getLogContext("markAvailable", Collections.singletonList(tableId));
        log.logInfo("Marking table as AVAILABLE ...!", logContext);

        TableEntity table = getTable(tableId, logContext);
        transitionStatus(table.getTableStatus(), TableStatus.CLEANING,
            "Only CLEANING tables can be marked AVAILABLE", tableId
        );

        table.setTableStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
        return modelMapper.map(table, TableModel.class);
    }

    // đánh dấu bàn đã đặt trước
    @Override
    public TableModel markReserved(Integer tableId) {
        LogContext logContext = getLogContext("markReserved", Collections.singletonList(tableId));
        log.logInfo("Marking table as RESERVED ...!", logContext);

        TableEntity table = getTable(tableId, logContext);
        transitionStatus(table.getTableStatus(), TableStatus.AVAILABLE,
            "Only AVAILABLE tables can be marked RESERVED", tableId
        );

        table.setTableStatus(TableStatus.RESERVED);
        tableRepository.save(table);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
        return modelMapper.map(table, TableModel.class);
    }

    // đánh dấu bàn đã có người
    @Override
    public TableModel markOccupied(Integer tableId) {
        LogContext logContext = getLogContext("markOccupied", Collections.singletonList(tableId));
        log.logInfo("Marking table as OCCUPIED ...!", logContext);

        TableEntity table = getTable(tableId, logContext);
        TableStatus s = table.getTableStatus();
        if (s != TableStatus.AVAILABLE && s != TableStatus.RESERVED) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "Only AVAILABLE or RESERVED tables can be marked OCCUPIED",
                "TableModel",
                "table must be AVAILABLE or RESERVED"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        table.setTableStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
        return modelMapper.map(table, TableModel.class);
    }

    // đánh dấu bàn đang dọn dẹp
    @Override
    public TableModel markCleaning(Integer tableId) {
        LogContext logContext = getLogContext("markCleaning", Collections.singletonList(tableId));
        log.logInfo("Marking table as CLEANING ...!", logContext);

        TableEntity table = getTable(tableId, logContext);
        transitionStatus(table.getTableStatus(), TableStatus.OCCUPIED,
            "Only OCCUPIED tables can be marked CLEANING", tableId
        );

        table.setTableStatus(TableStatus.CLEANING);
        tableRepository.save(table);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, TABLE_REDIS_KEY_PREFIX);
        return modelMapper.map(table, TableModel.class);
    }

    // private method

    private TableEntity getTable(Integer tableId, LogContext logContext) {
        return tableRepository.findById(tableId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Table not found with id: " + tableId,
                Collections.singletonList(tableId),
                "TableModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    // kiểm tra việc chuyển trạng thái có hợp lệ không
    private void transitionStatus(
        TableStatus current, TableStatus required, 
        String message, Integer tableId
    ) {
        LogContext logContext = getLogContext(
            "transitionStatus", Collections.singletonList(tableId)
        );
        if (current != required) {
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                message, 
                "TableModel", 
                "invalid table status transition for tableId=" + tableId
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
    }

    private List<FilterCondition<TableEntity>> buildFilterConditions(
        Integer id, Integer tableNumber, Integer capacity, 
        TableStatus tableStatus, String location
    ) {
        List<FilterCondition<TableEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(tableNumber != null) {
            conditions.add(FilterCondition.eq("tableNumber", tableNumber));
        }
        if(capacity != null) {
            conditions.add(FilterCondition.eq("capacity", capacity));
        }
        if(tableStatus != null) {
            conditions.add(FilterCondition.eq("tableStatus", tableStatus));
        }
        if(StringUtils.hasText(location)) {
            conditions.add(FilterCondition.likeIgnoreCase("location", location));
        }
        return conditions;
    }
}
