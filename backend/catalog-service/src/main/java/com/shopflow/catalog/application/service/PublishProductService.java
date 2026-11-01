package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.PublishProductUseCase;
import com.shopflow.catalog.application.api.command.PublishProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Retrouve le produit du vendeur, le publie, l'enregistre et l'annonce aux autres services. S'il était déjà publié,
 * il n'y a rien à enregistrer ni à annoncer.
 * <p>
 * Transactionnel : le statut et l'événement {@code ProductPublished} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class PublishProductService implements PublishProductUseCase {

    private final GetSellerProductUseCase getSellerProductUseCase;
    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public Product publishProduct(final PublishProductCommand command) {
        Product product = getSellerProductUseCase.getSellerProduct(command.getSellerId(), command.getProductId());
        Product published = product.publish();
        if (published == product) {
            return product;
        }
        productRepository.save(published);
        productEventPublisher.publishProductPublished(published);
        return published;
    }
}
