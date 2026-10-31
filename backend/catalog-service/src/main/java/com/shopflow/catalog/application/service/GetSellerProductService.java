package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Retrouve un produit du vendeur. Le produit d'un autre vendeur est traité comme inexistant.
 */
@Service
@RequiredArgsConstructor
public class GetSellerProductService implements GetSellerProductUseCase {

    private final ProductRepository productRepository;

    @Override
    public Product getSellerProduct(final SellerId sellerId, final ProductId productId) {
        return productRepository.findById(productId)
                .filter(product -> product.getSellerId().equals(sellerId))
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
