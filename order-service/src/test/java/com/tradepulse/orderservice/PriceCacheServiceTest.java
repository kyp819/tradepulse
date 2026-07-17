package com.tradepulse.orderservice;

import com.tradepulse.orderservice.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PriceCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PriceCacheService priceCacheService;
    private PriceCacheService priceCacheServiceNoRedis;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        priceCacheService = new PriceCacheService(redisTemplate);
        priceCacheServiceNoRedis = new PriceCacheService(null);
    }

    @Test
    void testUpdatePrice_withRedis_storesInCacheAndRedis() {
        priceCacheService.updatePrice("btcusdt", BigDecimal.valueOf(50000));

        verify(valueOperations).set(eq("price:BTCUSDT"), eq("50000"));
    }

    @Test
    void testUpdatePrice_withoutRedis_storesInLocalCacheOnly() {
        priceCacheServiceNoRedis.updatePrice("ETHUSDT", BigDecimal.valueOf(2000));

        BigDecimal price = priceCacheServiceNoRedis.getLatestPrice("ETHUSDT");
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(price));
    }

    @Test
    void testGetLatestPrice_fromRedis_returnsRedisValue() {
        when(valueOperations.get("price:BTCUSDT")).thenReturn("48000");

        BigDecimal price = priceCacheService.getLatestPrice("BTCUSDT");

        assertEquals(0, BigDecimal.valueOf(48000).compareTo(price));
    }

    @Test
    void testGetLatestPrice_redisMiss_fallsBackToLocalCache() {
        // Seed local cache via updatePrice
        when(valueOperations.get("price:BTCUSDT")).thenReturn(null);
        priceCacheService.updatePrice("BTCUSDT", BigDecimal.valueOf(47000));
        when(valueOperations.get("price:BTCUSDT")).thenReturn(null); // simulate Redis miss

        BigDecimal price = priceCacheService.getLatestPrice("BTCUSDT");

        assertNotNull(price);
        assertEquals(0, BigDecimal.valueOf(47000).compareTo(price));
    }

    @Test
    void testGetLatestPrice_noRedis_noCache_returnsNull() {
        BigDecimal price = priceCacheServiceNoRedis.getLatestPrice("XRPUSDT");
        assertNull(price);
    }

    @Test
    void testUpdatePrice_redisThrows_doesNotPropagate() {
        doThrow(new RuntimeException("Redis down")).when(valueOperations).set(any(), any());

        assertDoesNotThrow(() -> priceCacheService.updatePrice("BTCUSDT", BigDecimal.valueOf(1)));
    }

    @Test
    void testGetLatestPrice_redisThrows_fallsBackToLocalCache() {
        priceCacheService.updatePrice("ETHUSDT", BigDecimal.valueOf(3000));
        doThrow(new RuntimeException("Redis down")).when(valueOperations).get(any());

        BigDecimal price = priceCacheService.getLatestPrice("ETHUSDT");

        assertNotNull(price);
    }
}
