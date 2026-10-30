package com.shopflow.catalog.application;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Cas d'usage : un vendeur consulte un de ses produits.
 * Le produit d'un autre vendeur est traité comme inexistant, pour ne pas révéler qu'il existe.
 */
@Service
public class FindSellerProduct {

    private final ProductRepository products;

    public FindSellerProduct(ProductRepository products) {
        this.products = products;
    }

    public Optional<Product> handle(SellerId sellerId, ProductId productId) {
        return products.findById(productId).filter(product -> product.sellerId().equals(sellerId));
    }
}
