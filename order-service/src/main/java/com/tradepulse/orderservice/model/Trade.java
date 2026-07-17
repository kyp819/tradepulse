package com.tradepulse.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }
}
