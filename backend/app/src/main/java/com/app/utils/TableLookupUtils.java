package com.app.utils;

import java.util.Collections;

import com.common.entities.TableEntity;
import com.common.enums.TableStatus;
import com.common.repositories.TableRepository;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

public final class TableLookupUtils {

    private TableLookupUtils() {
    }

    public static TableEntity requireTable(
        TableRepository tableRepository,
        Integer tableNumber,
        String modelName,
        LogContext logContext,
        LoggingService log
    ) {
        return tableRepository.findByTableNumber(tableNumber).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Table not found with tableNumber: " + tableNumber,
                Collections.singletonList(tableNumber),
                modelName
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    public static TableEntity requireAvailableTable(
        TableRepository tableRepository,
        Integer tableNumber,
        String modelName,
        LogContext logContext,
        LoggingService log
    ) {
        return tableRepository.findByTableNumberAndTableStatus(tableNumber, TableStatus.AVAILABLE)
            .orElseThrow(() -> {
                ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                    "Table is not available: " + tableNumber + ", please choose another table",
                    modelName,
                    "table must be available"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            });
    }
}
