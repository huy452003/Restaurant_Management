package com.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableRetry(order = 100)
@EnableTransactionManagement(order = 200)
public class AopOrderConfig {}
