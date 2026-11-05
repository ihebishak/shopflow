package com.shopflow.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryNodeTest {

    private static final Category SHOES = Category.root(new CategorySlug("chaussures"), "Chaussures");
    private static final Category CLOTHES = Category.root(new CategorySlug("vetements"), "Vêtements");
    private static final Category BOOTS = SHOES.child(new CategorySlug("bottes"), "Bottes");
    private static final Category SNEAKERS = SHOES.child(new CategorySlug("sneakers"), "Sneakers");
    private static final Category RUNNING = SNEAKERS.child(new CategorySlug("running"), "Running");
    private static final Category TOPS = CLOTHES.child(new CategorySlug("hauts"), "Hauts");

    @Test
    @DisplayName("SHOULD return an empty tree WHEN there is no category")
    void shouldReturnEmptyTreeWhenThereIsNoCategory() {
        // WHEN
        List<CategoryNode> tree = CategoryNode.treeOf(List.of());

        // THEN
        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("SHOULD keep every first-level category without children WHEN nothing is below them")
    void shouldKeepFirstLevelCategoriesWhenNothingIsBelowThem() {
        // WHEN
        List<CategoryNode> tree = CategoryNode.treeOf(List.of(SHOES, CLOTHES));

        // THEN
        assertThat(slugsOf(tree)).containsExactly("chaussures", "vetements");
        assertThat(tree.get(0).getName()).isEqualTo("Chaussures");
        assertThat(tree.get(0).getChildren()).isEmpty();
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD place each category only under its direct parent WHEN the tree has three levels")
    void shouldPlaceCategoryUnderDirectParentWhenTreeHasThreeLevels() {
        // WHEN
        List<CategoryNode> tree = CategoryNode.treeOf(List.of(SHOES, RUNNING, SNEAKERS));

        // THEN
        assertThat(slugsOf(tree)).containsExactly("chaussures");
        CategoryNode shoes = tree.get(0);
        assertThat(slugsOf(shoes.getChildren())).containsExactly("sneakers");
        CategoryNode sneakers = shoes.getChildren().get(0);
        assertThat(slugsOf(sneakers.getChildren())).containsExactly("running");
        assertThat(sneakers.getChildren().get(0).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD keep the received order and the right parents WHEN siblings are mixed in the list")
    void shouldKeepOrderAndParentsWhenSiblingsAreMixed() {
        // WHEN
        List<CategoryNode> tree = CategoryNode.treeOf(List.of(BOOTS, SHOES, TOPS, SNEAKERS, CLOTHES));

        // THEN
        assertThat(slugsOf(tree)).containsExactly("chaussures", "vetements");
        assertThat(slugsOf(tree.get(0).getChildren())).containsExactly("bottes", "sneakers");
        assertThat(slugsOf(tree.get(1).getChildren())).containsExactly("hauts");
    }

    private static List<String> slugsOf(final List<CategoryNode> nodes) {
        return nodes.stream().map(node -> node.getSlug().getValue()).toList();
    }
}
