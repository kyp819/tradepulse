package com.tradepulse.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    @Id
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal averagePrice;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
