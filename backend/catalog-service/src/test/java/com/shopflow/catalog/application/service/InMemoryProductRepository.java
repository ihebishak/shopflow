package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductPage;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Faux dépôt en mémoire, pour tester les cas d'usage sans MongoDB.
 */
class InMemoryProductRepository implements ProductRepository {

    private final Map<ProductId, Product> products = new HashMap<>();

    @Override
    public void save(final Product product) {
        products.put(product.getId(), product);
    }

    @Override
    public Optional<Product> findById(final ProductId id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public ProductPage findBySeller(final SellerId sellerId, final ProductStatus status, final int page,
                                    final int size) {
        throw new UnsupportedOperationException("the tests using this fake list no products");
    }
}
