package com.tradepulse.orderservice.repository;

import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStatus(OrderStatus status);
    java.util.Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
