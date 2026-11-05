package com.shopflow.catalog.domain;

/**
 * À qui s'adresse un produit. Ce n'est pas une catégorie : « Sneakers » reste une seule catégorie de l'arbre,
 * que le client filtre par genre.
 */
public enum Gender {

    MEN,

    WOMEN,

    /** Pour tous, comme la plupart des sneakers. */
    UNISEX,

    KIDS
}
