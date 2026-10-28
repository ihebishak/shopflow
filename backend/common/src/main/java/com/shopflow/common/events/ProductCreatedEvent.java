package com.shopflow.common.events;

public record ProductCreatedEvent(String productId, String name, int initialStock) {
}
