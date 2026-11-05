package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.CategoryNotFoundException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;

/**
 * Crée un brouillon, l'enregistre et l'annonce aux autres services. Sa catégorie doit exister dans l'arbre.
 * <p>
 * Transactionnel : le produit et l'événement {@code ProductCreated} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductEventPublisher productEventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public Product createProduct(final CreateProductCommand command) {
        requireExistingCategory(command.getCategory().strip());
        Product product = Product.draft(command.getSellerId(), command.getName(), command.getBrand(),
                command.getCategory(), command.getDescription(), clock.instant());
        productRepository.save(product);
        productEventPublisher.publishProductCreated(product);
        return product;
    }

    /**
     * @throws com.shopflow.catalog.domain.InvalidCategorySlugException si l'identifiant est mal formé
     * @throws CategoryNotFoundException si aucune catégorie de l'arbre ne porte cet identifiant
     */
    private void requireExistingCategory(final String category) {
        CategorySlug slug = new CategorySlug(category);
        if (categoryRepository.findBySlug(slug).isEmpty()) {
            throw new CategoryNotFoundException(slug);
        }
    }
}
