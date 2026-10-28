package com.shopflow.common.events;

import java.math.BigDecimal;

public record PaymentCompletedEvent(String orderId, String customerId, BigDecimal amount, String transactionId) {
}
