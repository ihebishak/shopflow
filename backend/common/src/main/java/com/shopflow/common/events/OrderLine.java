package com.shopflow.common.events;

import java.math.BigDecimal;

public record OrderLine(String productId, String productName, int quantity, BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
