package com.shopflow.catalog.application;

import com.shopflow.catalog.domain.InvalidProductException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.SellerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cas d'usage : un vendeur crée un produit, qui naît en brouillon.
 * <p>
 * Transactionnel : l'événement {@code ProductCreated} (étape 3.5) sera écrit dans l'outbox
 * dans la même transaction que le produit.
 */
@Service
public class CreateProduct {

    private final ProductRepository products;
    private final Clock clock;

    public CreateProduct(ProductRepository products, Clock clock) {
        this.products = products;
        this.clock = clock;
    }

    /**
     * @throws InvalidProductException si une règle n'est pas respectée (rien n'est enregistré)
     */
    @Transactional
    public Product handle(Command command) {
        Product product = Product.draft(command.sellerId(), command.name(), command.brand(), command.category(),
                command.description(), clock.instant());
        products.save(product);
        return product;
    }

    /**
     * Demande de création d'un produit.
     *
     * @param description facultative
     */
    public record Command(SellerId sellerId, String name, String brand, String category, String description) {
    }
}
