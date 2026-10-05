package com.app.schedulers;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.app.services.TableStatusSyncService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TableStatusSyncOnStartup {

    private final TableStatusSyncService tableStatusSyncService;

    // đồng bộ trạng thái cho tất cả bàn khi application start up
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        tableStatusSyncService.reconcileAllTableStatuses();
    }
}
