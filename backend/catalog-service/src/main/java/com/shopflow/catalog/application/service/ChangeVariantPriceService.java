package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.ChangeVariantPriceUseCase;
import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.command.ChangeVariantPriceCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.Variant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Retrouve la variante du vendeur, change son prix, enregistre le produit et annonce l'ancien et le nouveau prix aux
 * autres services. Si le prix est le même, il n'y a rien à enregistrer ni à annoncer.
 * <p>
 * Transactionnel : le prix et l'événement {@code ProductPriceChanged} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ChangeVariantPriceService implements ChangeVariantPriceUseCase {

    private final GetSellerProductUseCase getSellerProductUseCase;
    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public Variant changeVariantPrice(final ChangeVariantPriceCommand command) {
        Product product = getSellerProductUseCase.getSellerProduct(command.getSellerId(), command.getProductId());
        Variant variant = product.variant(command.getSku());
        if (variant.getPrice().equals(command.getPrice())) {
            return variant;
        }
        Product updated = product.changeVariantPrice(command.getSku(), command.getPrice());
        productRepository.save(updated);
        productEventPublisher.publishProductPriceChanged(updated.getId(), command.getSku(), variant.getPrice(),
                command.getPrice());
        return updated.variant(command.getSku());
    }
}
