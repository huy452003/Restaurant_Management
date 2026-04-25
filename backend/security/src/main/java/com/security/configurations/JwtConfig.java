package com.security.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(String secret, Long expiration, Long refreshExpiration, Long verificationExpiration) {}
