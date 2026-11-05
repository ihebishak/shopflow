package com.shopflow.catalog.domain;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * Une catégorie rangée dans l'arbre, avec ses sous-catégories.
 */
@Getter
public final class CategoryNode {

    private final CategorySlug slug;
    private final String name;
    private final List<CategoryNode> children;

    private CategoryNode(final CategorySlug slug, final String name, final List<CategoryNode> children) {
        this.slug = Objects.requireNonNull(slug, "slug");
        this.name = Objects.requireNonNull(name, "name");
        this.children = List.copyOf(children);
    }

    /** Range une liste à plat : les catégories de premier niveau, chacune avec ses descendants, dans l'ordre reçu. */
    public static List<CategoryNode> treeOf(final List<Category> categories) {
        return categories.stream().filter(Category::isRoot)
                .map(root -> nodeOf(root, categories)).toList();
    }

    private static CategoryNode nodeOf(final Category category, final List<Category> categories) {
        List<CategoryNode> children = categories.stream().filter(other -> other.isChildOf(category))
                .map(child -> nodeOf(child, categories)).toList();
        return new CategoryNode(category.getSlug(), category.getName(), children);
    }
}
