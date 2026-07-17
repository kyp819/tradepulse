package com.tradepulse.orderservice;

import com.tradepulse.orderservice.controller.OrderController;
import com.tradepulse.orderservice.exception.ResourceNotFoundException;
import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderStatus;
import com.tradepulse.orderservice.model.Position;
import com.tradepulse.orderservice.repository.PositionRepository;
import com.tradepulse.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.tradepulse.orderservice.repository.AuditLogRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void testCancelOrder_Success() throws Exception {
        Order cancelled = Order.builder()
                .id("123")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder("123")).thenReturn(cancelled);

        mockMvc.perform(delete("/api/orders/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("123")))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        verify(orderService).cancelOrder("123");
    }

    @Test
    void testCancelOrder_NotFound() throws Exception {
        when(orderService.cancelOrder("999")).thenThrow(new ResourceNotFoundException("Order not found: 999"));

        mockMvc.perform(delete("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Order not found: 999")));

        verify(orderService).cancelOrder("999");
    }

    @Test
    void testGetOrder_Success() throws Exception {
        Order order = Order.builder()
                .id("123")
                .status(OrderStatus.PENDING)
                .symbol("BTCUSDT")
                .build();

        when(orderService.getOrder("123")).thenReturn(order);

        mockMvc.perform(get("/api/orders/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("123")))
                .andExpect(jsonPath("$.symbol", is("BTCUSDT")));

        verify(orderService).getOrder("123");
    }

    @Test
    void testGetPositions_Success() throws Exception {
        Position position = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.valueOf(1.5))
                .averagePrice(BigDecimal.valueOf(50000))
                .updatedAt(LocalDateTime.now())
                .build();

        when(positionRepository.findAll()).thenReturn(Collections.singletonList(position));

        mockMvc.perform(get("/api/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol", is("BTCUSDT")))
                .andExpect(jsonPath("$[0].quantity", is(1.5)));

        verify(positionRepository).findAll();
    }
}
