package com.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairMigrationStrategyConfig {

    @Bean
    @ConditionalOnProperty(name = "app.flyway.repair-before-migrate", havingValue = "true", matchIfMissing = true)
    public FlywayMigrationStrategy flywayRepairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
