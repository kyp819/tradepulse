package com.tradepulse.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.orderservice.event.PriceEventListener;
import com.tradepulse.orderservice.service.OrderExecutionEngine;
import com.tradepulse.orderservice.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PriceEventListenerTest {

    @Mock
    private PriceCacheService priceCacheService;

    @Mock
    private OrderExecutionEngine executionEngine;

    private PriceEventListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new PriceEventListener(priceCacheService, executionEngine, new ObjectMapper());
    }

    @Test
    void testConsumePrice_validJson_updatesCache() throws IOException {
        String json = "{\"symbol\":\"BTCUSDT\",\"price\":50000.00}";

        listener.consumePrice(json);

        verify(priceCacheService).updatePrice(eq("BTCUSDT"), any(BigDecimal.class));
        verify(executionEngine).matchPendingOrders(eq("BTCUSDT"), any(BigDecimal.class));
    }

    @Test
    void testConsumePrice_invalidJson_throwsException() {
        String poison = "not-a-valid-json";

        assertThrows(IOException.class, () -> listener.consumePrice(poison));

        verifyNoInteractions(priceCacheService, executionEngine);
    }

    @Test
    void testConsumePrice_missingFields_stillProcesses() throws IOException {
        // Partial JSON — objectMapper will use defaults/nulls
        String json = "{\"symbol\":\"ETHUSDT\",\"price\":2000.00}";

        listener.consumePrice(json);

        verify(priceCacheService).updatePrice(eq("ETHUSDT"), any(BigDecimal.class));
    }
}
