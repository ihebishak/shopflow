package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryAlreadyExistsException;
import com.shopflow.catalog.domain.CategorySlug;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Faux dépôt en mémoire, pour tester les cas d'usage sans MongoDB.
 */
class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<CategorySlug, Category> categories = new HashMap<>();

    @Override
    public void create(final Category category) {
        if (categories.putIfAbsent(category.getSlug(), category) != null) {
            throw new CategoryAlreadyExistsException(category.getSlug());
        }
    }

    @Override
    public Optional<Category> findBySlug(final CategorySlug slug) {
        return Optional.ofNullable(categories.get(slug));
    }

    @Override
    public List<Category> findAll() {
        return categories.values().stream()
                .sorted(Comparator.comparing(category -> category.getSlug().getValue())).toList();
    }
}
