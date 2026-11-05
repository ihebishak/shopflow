package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.ErrorKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CategoryTest {

    private static final CategorySlug CLOTHES = new CategorySlug("vetements");
    private static final CategorySlug TOPS = new CategorySlug("hauts");
    private static final CategorySlug T_SHIRTS = new CategorySlug("t-shirts");

    @Test
    @DisplayName("SHOULD have no ancestor WHEN the category is at the first level")
    void shouldHaveNoAncestorWhenCategoryIsAtFirstLevel() {
        // WHEN
        Category clothes = Category.root(CLOTHES, "  Vêtements  ");

        // THEN
        assertThat(clothes.getSlug()).isEqualTo(CLOTHES);
        assertThat(clothes.getName()).isEqualTo("Vêtements");
        assertThat(clothes.getAncestors()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD pass the ancestors down, highest first, WHEN children are created")
    void shouldPassAncestorsDownWhenChildrenAreCreated() {
        // GIVEN
        Category clothes = Category.root(CLOTHES, "Vêtements");

        // WHEN
        Category tops = clothes.child(TOPS, "Hauts");
        Category tShirts = tops.child(T_SHIRTS, "  T-shirts  ");

        // THEN
        assertThat(tops.getAncestors()).containsExactly(CLOTHES);
        assertThat(tShirts.getAncestors()).containsExactly(CLOTHES, TOPS);
        assertThat(tShirts.getName()).isEqualTo("T-shirts");
    }

    @Test
    @DisplayName("SHOULD refuse a child WHEN the category is already at the last level")
    void shouldRefuseChildWhenCategoryIsAtLastLevel() {
        // GIVEN
        Category tShirts = Category.root(CLOTHES, "Vêtements").child(TOPS, "Hauts").child(T_SHIRTS, "T-shirts");

        // WHEN
        Throwable thrown = catchThrowable(() -> tShirts.child(new CategorySlug("oversize"), "Oversize"));

        // THEN
        assertThat(thrown).isInstanceOf(CategoryTooDeepException.class)
                .hasFieldOrPropertyWithValue("kind", ErrorKind.CONFLICT)
                .hasFieldOrPropertyWithValue("code", CategoryTooDeepException.CODE)
                .hasMessageContaining("t-shirts");
    }

    @Test
    @DisplayName("SHOULD be a root only WHEN the category is at the first level")
    void shouldBeRootOnlyWhenCategoryIsAtFirstLevel() {
        // GIVEN
        Category clothes = Category.root(CLOTHES, "Vêtements");

        // WHEN / THEN
        assertThat(clothes.isRoot()).isTrue();
        assertThat(clothes.child(TOPS, "Hauts").isRoot()).isFalse();
    }

    @Test
    @DisplayName("SHOULD be the child of its direct parent only WHEN the tree has three levels")
    void shouldBeChildOfDirectParentOnlyWhenTreeHasThreeLevels() {
        // GIVEN
        Category clothes = Category.root(CLOTHES, "Vêtements");
        Category tops = clothes.child(TOPS, "Hauts");
        Category tShirts = tops.child(T_SHIRTS, "T-shirts");

        // WHEN / THEN
        assertThat(tShirts.isChildOf(tops)).isTrue();
        assertThat(tShirts.isChildOf(clothes)).as("grand-parent").isFalse();
        assertThat(tops.isChildOf(tShirts)).as("enfant").isFalse();
        assertThat(clothes.isChildOf(tops)).as("catégorie de premier niveau").isFalse();
    }

    @Test
    @DisplayName("SHOULD rebuild the same category WHEN it is restored")
    void shouldRebuildSameCategoryWhenRestored() {
        // WHEN
        Category restored = Category.restore(TOPS, "Hauts", List.of(CLOTHES));

        // THEN
        assertThat(restored.getSlug()).isEqualTo(TOPS);
        assertThat(restored.getName()).isEqualTo("Hauts");
        assertThat(restored.getAncestors()).containsExactly(CLOTHES);
    }
}
