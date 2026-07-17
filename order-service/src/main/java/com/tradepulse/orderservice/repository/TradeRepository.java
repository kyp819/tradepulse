package com.tradepulse.orderservice.repository;

import com.tradepulse.orderservice.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    List<Trade> findByOrderId(String orderId);
}
