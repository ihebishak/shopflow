package com.shopflow.catalog.domain;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Une catégorie de l'arbre du catalogue (ex. Chaussures > Sneakers). Elle garde tous ses parents : la recherche
 * pourra retrouver les produits d'une catégorie et de ses sous-catégories sans parcourir l'arbre.
 */
@Getter
public final class Category {

    public static final int MAX_DEPTH = 3;

    private final CategorySlug slug;
    private final String name;

    /** Les parents, du plus haut au plus proche ; vide pour une catégorie de premier niveau. */
    private final List<CategorySlug> ancestors;

    private Category(final CategorySlug slug, final String name, final List<CategorySlug> ancestors) {
        this.slug = Objects.requireNonNull(slug, "slug");
        this.name = Objects.requireNonNull(name, "name");
        this.ancestors = List.copyOf(ancestors);
    }

    /** Crée une catégorie de premier niveau. */
    public static Category root(final CategorySlug slug, final String name) {
        return new Category(slug, name.strip(), List.of());
    }

    /** Reconstruit une catégorie déjà enregistrée. */
    public static Category restore(final CategorySlug slug, final String name, final List<CategorySlug> ancestors) {
        return new Category(slug, name, ancestors);
    }

    /**
     * Crée une sous-catégorie de celle-ci.
     *
     * @throws CategoryTooDeepException si cette catégorie est déjà au dernier niveau de l'arbre
     */
    public Category child(final CategorySlug childSlug, final String childName) {
        if (ancestors.size() + 1 == MAX_DEPTH) {
            throw new CategoryTooDeepException(slug);
        }
        List<CategorySlug> childAncestors = new ArrayList<>(ancestors);
        childAncestors.add(slug);
        return new Category(childSlug, childName.strip(), childAncestors);
    }
}
