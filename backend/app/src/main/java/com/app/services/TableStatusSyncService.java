package com.app.services;

public interface TableStatusSyncService {

    void syncTableStatus(Integer tableNumber);

    void syncTableStatus(Integer tableNumber, Integer excludeReservationId);

    void reconcileAllTableStatuses();
}
