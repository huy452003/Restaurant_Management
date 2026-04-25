package com.appreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "com.appreactive.repositories")
@ComponentScan(basePackages = {"com.appreactive", "com.logging", "com.handle_exceptions"})
public class AppReactiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppReactiveApplication.class, args);
    }
}
