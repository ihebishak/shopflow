package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.ListSellerProductsUseCase;
import com.shopflow.catalog.application.api.query.ListSellerProductsQuery;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.ProductPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Lit une page des produits du vendeur. Rien n'est écrit : ni transaction, ni événement.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ListSellerProductsService implements ListSellerProductsUseCase {

    private final ProductRepository productRepository;

    @Override
    public ProductPage listSellerProducts(final ListSellerProductsQuery query) {
        return productRepository.findBySeller(query.getSellerId(), query.getStatus(), query.getPage(), query.getSize());
    }
}
