package com.tradepulse.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.orderservice.controller.OrderController;
import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderSide;
import com.tradepulse.orderservice.model.OrderType;
import com.tradepulse.orderservice.service.OrderService;
import com.tradepulse.orderservice.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.tradepulse.orderservice.repository.AuditLogRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class ValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private PositionRepository positionRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidationFailure_NegativeQuantity() throws Exception {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(-1.0)) // negative
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "valid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.errors.quantity", is("Quantity must be positive")));
    }

    @Test
    void testValidationFailure_MissingSymbol() throws Exception {
        Order request = Order.builder()
                .symbol("") // blank
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(1.0))
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "valid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.errors.symbol", is("Symbol is required")));
    }
}
