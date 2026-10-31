package com.shopflow.catalog.infrastructure.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Forme d'une variante dans MongoDB, imbriquée dans {@link ProductDocument} : elle n'existe pas sans son produit.
 */
@Getter
@Builder
@AllArgsConstructor
public class VariantDocument {

    /** UUID v7 de la variante, en texte. */
    private final String sku;

    private final String size;
    private final String color;

    /** Montant en unités mineures (ex. 16000 = 160,00 €). */
    private final long priceAmount;

    /** Code ISO de la devise (ex. {@code EUR}). */
    private final String priceCurrency;
}
