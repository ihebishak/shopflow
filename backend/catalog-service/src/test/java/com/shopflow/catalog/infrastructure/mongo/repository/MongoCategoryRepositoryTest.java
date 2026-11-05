package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.catalog.domain.Category;
import com.shopflow.catalog.domain.CategoryAlreadyExistsException;
import com.shopflow.catalog.domain.CategorySlug;
import com.shopflow.catalog.infrastructure.mongo.document.CategoryDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.CategoryDocumentMapperImpl;
import com.shopflow.kernel.error.ErrorKind;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * L'adaptateur avec un vrai MongoDB : une catégorie créée puis relue est identique, et un identifiant reste unique.
 */
@Testcontainers
class MongoCategoryRepositoryTest {

    private static final CategorySlug SHOES = new CategorySlug("chaussures");
    private static final CategorySlug SNEAKERS = new CategorySlug("sneakers");

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static MongoClient mongoClient;
    private MongoTemplate mongoTemplate;
    private MongoCategoryRepository mongoCategoryRepository;

    @BeforeAll
    static void connect() {
        mongoClient = MongoClients.create(MONGO.getReplicaSetUrl());
    }

    @AfterAll
    static void disconnect() {
        mongoClient.close();
    }

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate = new MongoTemplate(mongoClient, "catalog-test");
        mongoTemplate.remove(new Query(), CategoryDocument.COLLECTION);
        mongoCategoryRepository = new MongoCategoryRepository(mongoTemplate, new CategoryDocumentMapperImpl());
    }

    @Test
    @DisplayName("SHOULD read back exactly the same category WHEN it has been created")
    void shouldReadBackSameCategoryWhenCreated() {
        // GIVEN
        Category sneakers = Category.root(SHOES, "Chaussures").child(SNEAKERS, "Sneakers");

        // WHEN
        mongoCategoryRepository.create(sneakers);
        Optional<Category> found = mongoCategoryRepository.findBySlug(SNEAKERS);

        // THEN
        assertThat(found).get().usingRecursiveComparison().isEqualTo(sneakers);
    }

    @Test
    @DisplayName("SHOULD store a readable document in the categories collection WHEN a category is created")
    void shouldStoreReadableDocumentWhenCategoryIsCreated() {
        // GIVEN
        Category sneakers = Category.root(SHOES, "Chaussures").child(SNEAKERS, "Sneakers");

        // WHEN
        mongoCategoryRepository.create(sneakers);

        // THEN
        Document stored = mongoTemplate.getCollection("categories").find().first();
        assertThat(stored).isNotNull()
                .containsEntry("_id", "sneakers")
                .containsEntry("name", "Sneakers")
                .containsEntry("ancestors", List.of("chaussures"));
    }

    @Test
    @DisplayName("SHOULD read back an empty ancestors list WHEN the category is at the first level")
    void shouldReadBackEmptyAncestorsWhenCategoryIsAtFirstLevel() {
        // GIVEN
        mongoCategoryRepository.create(Category.root(SHOES, "Chaussures"));

        // WHEN
        Optional<Category> found = mongoCategoryRepository.findBySlug(SHOES);

        // THEN
        assertThat(found).get().extracting(Category::getAncestors).isEqualTo(List.of());
    }

    @Test
    @DisplayName("SHOULD return nothing WHEN the category does not exist")
    void shouldReturnNothingWhenCategoryDoesNotExist() {
        // WHEN
        Optional<Category> found = mongoCategoryRepository.findBySlug(SNEAKERS);

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the category and keep the original WHEN the slug is already taken")
    void shouldRefuseCategoryAndKeepOriginalWhenSlugIsTaken() {
        // GIVEN
        mongoCategoryRepository.create(Category.root(SHOES, "Chaussures"));

        // WHEN
        Throwable thrown = catchThrowable(() -> mongoCategoryRepository.create(Category.root(SHOES, "Souliers")));

        // THEN
        assertThat(thrown).isInstanceOf(CategoryAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("kind", ErrorKind.CONFLICT)
                .hasFieldOrPropertyWithValue("code", CategoryAlreadyExistsException.CODE);
        assertThat(mongoCategoryRepository.findBySlug(SHOES)).get().extracting(Category::getName)
                .isEqualTo("Chaussures");
    }

    @Test
    @DisplayName("SHOULD return every category sorted by slug WHEN all categories are read")
    void shouldReturnEveryCategorySortedBySlugWhenAllAreRead() {
        // GIVEN
        mongoCategoryRepository.create(Category.root(new CategorySlug("vetements"), "Vêtements"));
        mongoCategoryRepository.create(Category.root(new CategorySlug("accessoires"), "Accessoires"));
        mongoCategoryRepository.create(Category.root(SHOES, "Chaussures"));

        // WHEN
        List<Category> categories = mongoCategoryRepository.findAll();

        // THEN
        assertThat(categories).extracting(category -> category.getSlug().getValue())
                .containsExactly("accessoires", "chaussures", "vetements");
    }
}
