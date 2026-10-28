package com.shopflow.common.events;

public record PaymentFailedEvent(String orderId, String customerId, String reason) {
}
