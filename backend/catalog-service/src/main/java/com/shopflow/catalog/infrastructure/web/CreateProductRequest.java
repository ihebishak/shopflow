package com.shopflow.catalog.infrastructure.web;

import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.SellerId;

/**
 * Corps JSON de {@code POST /api/v1/seller/products}. Les règles sont vérifiées par le domaine.
 *
 * @param description facultative
 */
record CreateProductRequest(String name, String brand, String category, String description) {

    CreateProductCommand toCommand(SellerId sellerId) {
        return CreateProductCommand.builder()
                .sellerId(sellerId)
                .name(name)
                .brand(brand)
                .category(category)
                .description(description)
                .build();
    }
}
