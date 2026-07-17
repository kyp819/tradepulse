package com.tradepulse.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEvent {
    private String symbol;
    private BigDecimal price;
    private Long timestamp;
}
