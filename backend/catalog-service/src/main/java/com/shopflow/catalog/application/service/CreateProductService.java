package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Crée un brouillon et l'enregistre.
 * <p>
 * Transactionnel : l'événement {@code ProductCreated} (étape 3.5) sera écrit dans l'outbox
 * dans la même transaction que le produit.
 */
@Service
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepository productRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Product createProduct(final CreateProductCommand command) {
        Product product = Product.draft(command.getSellerId(), command.getName(), command.getBrand(),
                command.getCategory(), command.getDescription(), clock.instant());
        productRepository.save(product);
        return product;
    }
}
