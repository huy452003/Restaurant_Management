package com.app.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.app.services.TableStatusSyncService;

@Component
public class TableStatusSyncOnStartup {

    @Autowired
    private TableStatusSyncService tableStatusSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        tableStatusSyncService.reconcileAllTableStatuses();
    }
}
