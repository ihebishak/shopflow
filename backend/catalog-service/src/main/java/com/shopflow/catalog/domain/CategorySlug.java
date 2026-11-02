package com.shopflow.catalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identifiant lisible d'une catégorie (ex. {@code hoodies-sweats}). Un type dédié empêche de le confondre avec un
 * autre texte, et sa longueur garantit qu'il tient dans le champ catégorie d'un produit.
 */
@Getter
@EqualsAndHashCode
public final class CategorySlug {

    public static final int MAX_LENGTH = Product.CATEGORY_MAX_LENGTH;

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private final String value;

    /**
     * @throws InvalidCategorySlugException si l'identifiant est trop long ou mal formé
     */
    public CategorySlug(final String value) {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidCategorySlugException();
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
