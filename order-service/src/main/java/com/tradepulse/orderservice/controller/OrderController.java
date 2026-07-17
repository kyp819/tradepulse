package com.tradepulse.orderservice.controller;

import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.Position;
import com.tradepulse.orderservice.model.AuditLog;
import com.tradepulse.orderservice.repository.PositionRepository;
import com.tradepulse.orderservice.repository.AuditLogRepository;
import com.tradepulse.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final PositionRepository positionRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public OrderController(OrderService orderService,
                           PositionRepository positionRepository,
                           AuditLogRepository auditLogRepository) {
        this.orderService = orderService;
        this.positionRepository = positionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody Order orderRequest) {
        Order order = orderService.placeOrder(orderRequest, idempotencyKey);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Order> cancelOrder(@PathVariable("id") String id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable("id") String id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getPositions() {
        return ResponseEntity.ok(positionRepository.findAll());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/settlement")
    public ResponseEntity<String> runSettlement() {
        String result = orderService.runSettlement();
        return ResponseEntity.ok(result);
    }
}
