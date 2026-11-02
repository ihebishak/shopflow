package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.CreateCategoryUseCase;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateCategoryRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryResponseDto;
import com.shopflow.catalog.infrastructure.web.mapper.CategoryRestMapper;
import com.shopflow.catalog.infrastructure.web.swagger.AdminCategoryControllerDocumentation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API des catégories côté admin : convertit (mapper) et délègue au cas d'usage, sans logique métier.
 * Les erreurs sont transformées en Problem Details par le gestionnaire du starter.
 * <p>
 * Route ouverte à tous en attendant Keycloak : l'étape 2.3 la réservera au rôle admin.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController implements AdminCategoryControllerDocumentation {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final CategoryRestMapper categoryRestMapper;

    /** Ajoute une catégorie à l'arbre. */
    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto createCategory(@Valid @RequestBody final CreateCategoryRequestDto requestDto) {
        Category category = createCategoryUseCase.createCategory(categoryRestMapper.toCommand(requestDto));
        return categoryRestMapper.toResponseDto(category);
    }
}
