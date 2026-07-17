package com.tradepulse.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @NotBlank(message = "Symbol is required")
    @Column(nullable = false)
    private String symbol;

    @NotNull(message = "Order side is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @NotNull(message = "Order type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Column(precision = 18, scale = 8)
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
