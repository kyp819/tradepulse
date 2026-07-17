package com.tradepulse.priceingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.priceingestion.service.PricePublisher;
import com.tradepulse.priceingestion.websocket.BinanceWebSocketClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class BinanceClientUnitTest {

    @Test
    void testParseBinanceTickerPayload() throws Exception {
        String json = "{\"e\":\"24hrTicker\",\"E\":1672531200000,\"s\":\"BTCUSDT\",\"c\":\"16800.50\"}";
        ObjectMapper objectMapper = new ObjectMapper();

        PricePublisher.BinanceTicker ticker = objectMapper.readValue(json, PricePublisher.BinanceTicker.class);

        assertNotNull(ticker);
        assertEquals("BTCUSDT", ticker.getSymbol());
        assertEquals("16800.50", ticker.getLastPrice());
        assertEquals(1672531200000L, ticker.getEventTime());
    }

    @Test
    void testWebSocketClientSchedulesReconnectOnClose() throws Exception {
        PricePublisher publisher = Mockito.mock(PricePublisher.class);
        ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        BinanceWebSocketClient client = new BinanceWebSocketClient(
                "ws://localhost:8080",
                publisher,
                executorService
        );

        WebSocketHandler handler = client.getWebSocketHandler();
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Verify that schedule reconnect was called and scheduled connect task
        verify(executorService).schedule(any(Runnable.class), eq(5L), eq(TimeUnit.SECONDS));
    }
}
