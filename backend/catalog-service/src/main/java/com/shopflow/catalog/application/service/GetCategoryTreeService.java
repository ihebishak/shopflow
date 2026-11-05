package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.GetCategoryTreeUseCase;
import com.shopflow.catalog.application.spi.CategoryRepository;
import com.shopflow.catalog.domain.CategoryNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lit toutes les catégories d'un coup, puis les range en arbre.
 */
@Service
@RequiredArgsConstructor
public class GetCategoryTreeService implements GetCategoryTreeUseCase {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryNode> getCategoryTree() {
        return CategoryNode.treeOf(categoryRepository.findAll());
    }
}
