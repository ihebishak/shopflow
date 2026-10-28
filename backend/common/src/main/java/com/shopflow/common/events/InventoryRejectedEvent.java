package com.shopflow.common.events;

public record InventoryRejectedEvent(String orderId, String customerId, String reason) {
}
