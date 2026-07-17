package com.tradepulse.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PriceCacheService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, BigDecimal> localCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public PriceCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updatePrice(String symbol, BigDecimal price) {
        String cleanSymbol = symbol.toUpperCase();
        localCache.put(cleanSymbol, price);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("price:" + cleanSymbol, price.toString());
            } catch (Exception e) {
                log.warn("Failed to update Redis cache for symbol {}: {}", cleanSymbol, e.getMessage());
            }
        }
    }

    public BigDecimal getLatestPrice(String symbol) {
        String cleanSymbol = symbol.toUpperCase();
        if (redisTemplate != null) {
            try {
                String priceStr = redisTemplate.opsForValue().get("price:" + cleanSymbol);
                if (priceStr != null) {
                    BigDecimal price = new BigDecimal(priceStr);
                    localCache.put(cleanSymbol, price);
                    return price;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch price from Redis for symbol {}: {}", cleanSymbol, e.getMessage());
            }
        }
        return localCache.get(cleanSymbol);
    }
}
