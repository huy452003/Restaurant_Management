package com.common.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.io.IOException;

@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        Config config = new Config();
        
        // Sử dụng environment variables cho Redis connection
        // Format: REDIS_URL=redis://host:port hoặc redis://:password@host:port
        String redisUrl = System.getenv("REDIS_URL");
        
        // Validate REDIS_URL - phải là URL hợp lệ, không phải text/comment
        boolean isValidRedisUrl = redisUrl != null && !redisUrl.isEmpty() && 
                                  !redisUrl.contains("(") && !redisUrl.contains(")") &&
                                  !redisUrl.contains("hoặc") && !redisUrl.contains("từ");
        
        if (isValidRedisUrl) {
            // Đảm bảo REDIS_URL có prefix redis:// hoặc rediss://
            String address = redisUrl;
            if (!redisUrl.startsWith("redis://") && !redisUrl.startsWith("rediss://")) {
                // Nếu không có prefix, thêm redis://
                address = "redis://" + redisUrl;
            }
            config.useSingleServer().setAddress(address);
        } else {
            // Fallback: dùng REDIS_HOST và REDIS_PORT hoặc localhost
            String redisHost = System.getenv("REDIS_HOST");
            String redisPort = System.getenv("REDIS_PORT");
            
            if (redisHost != null && !redisHost.isEmpty() && 
                !redisHost.contains("(") && !redisHost.contains("hoặc")) {
                String port = (redisPort != null && !redisPort.isEmpty() && 
                              !redisPort.contains("(") && !redisPort.contains("hoặc")) 
                              ? redisPort : "6379";
                config.useSingleServer().setAddress("redis://" + redisHost + ":" + port);
            } else {
                // Default fallback cho local development
                config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            }
        }
        
        // Set password nếu có
        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper.copy());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean("customStringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Sử dụng String serializer cho key và value
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper.copy());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }


}
