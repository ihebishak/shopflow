package com.shopflow.catalog.infrastructure.web.mapper;

import com.shopflow.catalog.application.api.command.CreateCategoryCommand;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryNode;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.domain.InvalidCategorySlugException;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateCategoryRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryNodeResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Le code généré par MapStruct, testé sans Spring.
 */
class CategoryRestMapperTest {

    private static final CategorySlug SHOES = new CategorySlug("chaussures");
    private static final CategorySlug SNEAKERS = new CategorySlug("sneakers");

    private final CategoryRestMapper categoryRestMapper = new CategoryRestMapperImpl();

    @Test
    @DisplayName("SHOULD build the command WHEN mapping a request with a parent")
    void shouldBuildCommandWhenRequestHasParent() {
        // GIVEN
        CreateCategoryRequestDto requestDto = CreateCategoryRequestDto.builder()
                .slug("sneakers").name("Sneakers").parentSlug("chaussures").build();

        // WHEN
        CreateCategoryCommand command = categoryRestMapper.toCommand(requestDto);

        // THEN
        assertThat(command.getSlug()).isEqualTo(SNEAKERS);
        assertThat(command.getName()).isEqualTo("Sneakers");
        assertThat(command.getParentSlug()).isEqualTo(SHOES);
    }

    @Test
    @DisplayName("SHOULD leave the parent empty WHEN the request has none")
    void shouldLeaveParentEmptyWhenRequestHasNone() {
        // GIVEN
        CreateCategoryRequestDto requestDto = CreateCategoryRequestDto.builder()
                .slug("chaussures").name("Chaussures").build();

        // WHEN
        CreateCategoryCommand command = categoryRestMapper.toCommand(requestDto);

        // THEN
        assertThat(command.getParentSlug()).isNull();
    }

    @Test
    @DisplayName("SHOULD refuse the request WHEN a slug is badly formed")
    void shouldRefuseRequestWhenSlugIsBadlyFormed() {
        // GIVEN
        CreateCategoryRequestDto requestDto = CreateCategoryRequestDto.builder()
                .slug("Sneakers").name("Sneakers").build();

        // WHEN
        Throwable thrown = catchThrowable(() -> categoryRestMapper.toCommand(requestDto));

        // THEN
        assertThat(thrown).isInstanceOf(InvalidCategorySlugException.class);
    }

    @Test
    @DisplayName("SHOULD copy the slug, the name and the ancestors WHEN mapping a category")
    void shouldCopyFieldsWhenMappingCategory() {
        // GIVEN
        Category sneakers = Category.root(SHOES, "Chaussures").child(SNEAKERS, "Sneakers");

        // WHEN
        CategoryResponseDto responseDto = categoryRestMapper.toResponseDto(sneakers);

        // THEN
        assertThat(responseDto.getSlug()).isEqualTo("sneakers");
        assertThat(responseDto.getName()).isEqualTo("Sneakers");
        assertThat(responseDto.getAncestors()).containsExactly("chaussures");
    }

    @Test
    @DisplayName("SHOULD copy each node and its children WHEN mapping a tree")
    void shouldCopyEachNodeAndItsChildrenWhenMappingTree() {
        // GIVEN
        Category shoes = Category.root(SHOES, "Chaussures");
        List<CategoryNode> tree = CategoryNode.treeOf(List.of(shoes, shoes.child(SNEAKERS, "Sneakers")));

        // WHEN
        List<CategoryNodeResponseDto> responseDtos = categoryRestMapper.toResponseDtos(tree);

        // THEN
        assertThat(responseDtos).hasSize(1);
        assertThat(responseDtos.get(0).getSlug()).isEqualTo("chaussures");
        assertThat(responseDtos.get(0).getName()).isEqualTo("Chaussures");
        CategoryNodeResponseDto child = responseDtos.get(0).getChildren().get(0);
        assertThat(child.getSlug()).isEqualTo("sneakers");
        assertThat(child.getName()).isEqualTo("Sneakers");
        assertThat(child.getChildren()).isEmpty();
    }
}
