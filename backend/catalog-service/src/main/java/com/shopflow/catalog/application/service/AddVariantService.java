package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.AddVariantUseCase;
import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Retrouve le produit du vendeur, y ajoute la variante, l'enregistre et l'annonce aux autres services.
 * <p>
 * Transactionnel : la variante et l'événement {@code ProductVariantAdded} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class AddVariantService implements AddVariantUseCase {

    private final GetSellerProductUseCase getSellerProductUseCase;
    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public Variant addVariant(final AddVariantCommand command) {
        Product product = getSellerProductUseCase.getSellerProduct(command.getSellerId(), command.getProductId());
        Variant variant = product.addVariant(command.getSize(), command.getColor(), command.getPrice());
        productRepository.save(product);
        productEventPublisher.publishProductVariantAdded(product.getId(), variant);
        return variant;
    }
}
