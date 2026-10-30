package com.shopflow.catalog.infrastructure.web;

import com.shopflow.catalog.application.CreateProduct;
import com.shopflow.catalog.domain.SellerId;

/**
 * Corps JSON de {@code POST /api/v1/seller/products}. Les règles sont vérifiées par le domaine.
 *
 * @param description facultative
 */
record CreateProductRequest(String name, String brand, String category, String description) {

    CreateProduct.Command toCommand(SellerId sellerId) {
        return new CreateProduct.Command(sellerId, name, brand, category, description);
    }
}
