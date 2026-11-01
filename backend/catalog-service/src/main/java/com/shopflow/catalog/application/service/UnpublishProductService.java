package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.UnpublishProductUseCase;
import com.shopflow.catalog.application.api.command.UnpublishProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.UnpublishReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Retrouve le produit du vendeur, le dépublie, l'enregistre et annonce son retrait aux autres services, avec le motif
 * {@code SELLER}. S'il était déjà dépublié, il n'y a rien à enregistrer ni à annoncer.
 * <p>
 * Transactionnel : le statut et l'événement {@code ProductUnpublished} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UnpublishProductService implements UnpublishProductUseCase {

    private final GetSellerProductUseCase getSellerProductUseCase;
    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public Product unpublishProduct(final UnpublishProductCommand command) {
        Product product = getSellerProductUseCase.getSellerProduct(command.getSellerId(), command.getProductId());
        Product unpublished = product.unpublish();
        if (unpublished == product) {
            return product;
        }
        productRepository.save(unpublished);
        productEventPublisher.publishProductUnpublished(unpublished, UnpublishReason.SELLER);
        return unpublished;
    }
}
