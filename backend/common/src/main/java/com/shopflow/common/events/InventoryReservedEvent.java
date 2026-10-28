package com.shopflow.common.events;

import java.math.BigDecimal;

public record InventoryReservedEvent(String orderId, String customerId, BigDecimal total) {
}
