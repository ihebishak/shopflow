package com.shopflow.catalog.infrastructure.web.mapper;

import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNode;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateCategoryRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryNodeResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Conversions entre les DTO de l'API et la couche application. Le code est généré par MapStruct à la compilation
 * ({@code CategoryRestMapperImpl}) ; les méthodes {@code default} lui expliquent les types du domaine.
 */
@Mapper
public interface CategoryRestMapper {

    CreateCategoryCommand toCommand(CreateCategoryRequestDto requestDto);

    CategoryResponseDto toResponseDto(Category category);

    CategoryNodeResponseDto toResponseDto(CategoryNode node);

    List<CategoryNodeResponseDto> toResponseDtos(List<CategoryNode> nodes);

    default String map(final CategorySlug slug) {
        return slug.getValue();
    }

    /**
     * @throws com.shopflow.catalog.domain.InvalidCategorySlugException si l'identifiant est mal formé
     */
    default CategorySlug toSlug(final String slug) {
        return slug == null ? null : new CategorySlug(slug);
    }
}
