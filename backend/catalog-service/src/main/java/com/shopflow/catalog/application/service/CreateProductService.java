package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Crée un brouillon, l'enregistre et l'annonce aux autres services.
 * <p>
 * Transactionnel : le produit et l'événement {@code ProductCreated} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public Product createProduct(final CreateProductCommand command) {
        Product product = Product.draft(command.getSellerId(), command.getName(), command.getBrand(),
                command.getCategory(), command.getDescription(), clock.instant());
        productRepository.save(product);
        productEventPublisher.publishProductCreated(product);
        return product;
    }
}
