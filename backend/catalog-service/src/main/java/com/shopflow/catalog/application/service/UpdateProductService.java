package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.UpdateProductUseCase;
import com.shopflow.catalog.application.api.command.UpdateProductCommand;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Retrouve le produit du vendeur, le modifie, l'enregistre et annonce les champs modifiés aux autres services.
 * Si rien n'a changé, il n'y a rien à enregistrer ni à annoncer.
 * <p>
 * Transactionnel : le produit et l'événement {@code ProductUpdated} sont enregistrés ensemble, ou pas du tout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateProductService implements UpdateProductUseCase {

    private final GetSellerProductUseCase getSellerProductUseCase;
    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public Product updateProduct(final UpdateProductCommand command) {
        Product product = getSellerProductUseCase.getSellerProduct(command.getSellerId(), command.getProductId());
        Product updated = product.update(command.getName(), command.getBrand(), command.getCategory(),
                command.getDescription());
        List<String> changedFields = updated.changedFieldsSince(product);
        if (changedFields.isEmpty()) {
            return product;
        }
        productRepository.save(updated);
        productEventPublisher.publishProductUpdated(updated.getId(), changedFields);
        return updated;
    }
}
