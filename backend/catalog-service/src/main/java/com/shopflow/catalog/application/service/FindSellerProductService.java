package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.FindSellerProductUseCase;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Retrouve un produit du vendeur. Le produit d'un autre vendeur est traité comme inexistant.
 */
@Service
@RequiredArgsConstructor
public class FindSellerProductService implements FindSellerProductUseCase {

    private final ProductRepository productRepository;

    @Override
    public Optional<Product> findSellerProduct(final SellerId sellerId, final ProductId productId) {
        return productRepository.findById(productId)
                .filter(product -> product.getSellerId().equals(sellerId));
    }
}
