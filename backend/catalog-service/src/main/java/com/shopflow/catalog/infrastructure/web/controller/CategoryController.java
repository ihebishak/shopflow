package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.GetCategoryTreeUseCase;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryNodeResponseDto;
import com.shopflow.catalog.infrastructure.web.mapper.CategoryRestMapper;
import com.shopflow.catalog.infrastructure.web.swagger.CategoryControllerDocumentation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API publique des catégories : convertit (mapper) et délègue au cas d'usage, sans logique métier.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocumentation {

    private final GetCategoryTreeUseCase getCategoryTreeUseCase;
    private final CategoryRestMapper categoryRestMapper;

    /** L'arbre complet des catégories. */
    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryNodeResponseDto> getCategoryTree() {
        return categoryRestMapper.toResponseDtos(getCategoryTreeUseCase.getCategoryTree());
    }
}
