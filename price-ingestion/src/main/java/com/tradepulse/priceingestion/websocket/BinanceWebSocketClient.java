package com.tradepulse.priceingestion.websocket;

import com.tradepulse.priceingestion.service.PricePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BinanceWebSocketClient {

    private final String webSocketUrl;
    private final PricePublisher pricePublisher;
    private final ScheduledExecutorService executorService;
    private WebSocketSession currentSession;

    @Autowired
    private org.springframework.core.env.Environment env;

    @Autowired
    public BinanceWebSocketClient(@Value("${app.binance.websocket-url}") String webSocketUrl,
                                   PricePublisher pricePublisher) {
        this.webSocketUrl = webSocketUrl;
        this.pricePublisher = pricePublisher;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    // Constructor for testing
    public BinanceWebSocketClient(String webSocketUrl, PricePublisher pricePublisher, ScheduledExecutorService executorService) {
        this.webSocketUrl = webSocketUrl;
        this.pricePublisher = pricePublisher;
        this.executorService = executorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        if (env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("test")) {
            log.info("Test profile active. Skipping real Binance WebSocket connection.");
            return;
        }

        log.info("Connecting to Binance WebSocket URL: {}", webSocketUrl);
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHandler handler = getWebSocketHandler();

        try {
            client.execute(handler, webSocketUrl);
        } catch (Exception e) {
            log.error("Failed to execute WebSocket client connection: {}. Reconnecting...", e.getMessage());
            scheduleReconnect();
        }
    }

    public WebSocketHandler getWebSocketHandler() {
        return new BinanceWebSocketHandler();
    }

    public void scheduleReconnect() {
        executorService.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    public WebSocketSession getCurrentSession() {
        return currentSession;
    }

    class BinanceWebSocketHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Successfully connected to Binance WebSocket!");
            currentSession = session;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();
            log.debug("Received WebSocket payload: {}", payload);
            pricePublisher.processAndPublish(payload);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("Binance WebSocket connection closed with status: {}. Reconnecting...", status);
            currentSession = null;
            scheduleReconnect();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Binance WebSocket transport error: {}. Closing session...", exception.getMessage());
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed to close session: {}", e.getMessage());
            }
        }
    }
}
