package com.shopflow.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(String orderId, String customerId, List<OrderLine> lines, BigDecimal total,
                                Instant createdAt) {
}
