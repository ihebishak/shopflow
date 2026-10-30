package com.shopflow.catalog.domain;

/**
 * Cycle de vie d'un produit : brouillon → publié → dépublié.
 */
public enum ProductStatus {

    /** En préparation par le vendeur, invisible des clients. */
    DRAFT,

    /** En vente, visible des clients. */
    PUBLISHED,

    /** Retiré de la vente par le vendeur ou par l'admin. */
    UNPUBLISHED
}
