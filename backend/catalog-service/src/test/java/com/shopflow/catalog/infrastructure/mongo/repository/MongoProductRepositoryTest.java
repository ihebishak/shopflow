package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.catalog.domain.DuplicateVariantException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapperImpl;
import com.shopflow.kernel.money.Money;
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

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * L'adaptateur avec un vrai MongoDB : un produit enregistré puis relu est identique.
 */
@Testcontainers
class MongoProductRepositoryTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Money PRICE = Money.euros(16_000);

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static MongoClient mongoClient;
    private MongoTemplate mongoTemplate;
    private MongoProductRepository mongoProductRepository;

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
        mongoTemplate.remove(new Query(), ProductDocument.COLLECTION);
        mongoProductRepository = new MongoProductRepository(mongoTemplate, new ProductDocumentMapperImpl());
    }

    @Test
    @DisplayName("SHOULD read back exactly the same product WHEN it has been saved")
    void shouldReadBackSameProductWhenSaved() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers",
                "La réédition du modèle de 1990", Instant.now());

        // WHEN
        mongoProductRepository.save(airMax);
        Optional<Product> found = mongoProductRepository.findById(airMax.getId());

        // THEN
        assertThat(found).get().usingRecursiveComparison().isEqualTo(airMax);
    }

    @Test
    @DisplayName("SHOULD store a readable document in the products collection WHEN a product is saved")
    void shouldStoreReadableDocumentWhenProductIsSaved() {
        // GIVEN
        Instant createdAt = Instant.parse("2026-10-31T10:00:00Z");
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, createdAt);

        // WHEN
        mongoProductRepository.save(airMax);

        // THEN
        Document stored = mongoTemplate.getCollection("products").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.get("_id")).isEqualTo(airMax.getId().toString());
        assertThat(stored).containsEntry("sellerId", "seller-karim")
                .containsEntry("name", "Air Max 90 Infrared")
                .containsEntry("brand", "Nike")
                .containsEntry("category", "sneakers")
                .containsEntry("status", "DRAFT")
                .containsEntry("createdAt", Date.from(createdAt))
                .doesNotContainKey("description");
    }

    @Test
    @DisplayName("SHOULD find nothing WHEN the id is unknown")
    void shouldFindNothingWhenIdIsUnknown() {
        // WHEN
        Optional<Product> found = mongoProductRepository.findById(ProductId.newId());

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("SHOULD read back the same variants WHEN a product with variants has been saved")
    void shouldReadBackSameVariantsWhenSaved() {
        // GIVEN
        Product airMax = draftAirMax();
        airMax.addVariant("42", "Rouge", PRICE);
        airMax.addVariant("43", "Noir", Money.euros(17_000));

        // WHEN
        mongoProductRepository.save(airMax);
        Optional<Product> found = mongoProductRepository.findById(airMax.getId());

        // THEN
        assertThat(found).get().usingRecursiveComparison().isEqualTo(airMax);
    }

    @Test
    @DisplayName("SHOULD store the variants inside the product document WHEN a product is saved")
    void shouldStoreVariantsInsideProductDocumentWhenSaved() {
        // GIVEN
        Product airMax = draftAirMax();
        Variant variant = airMax.addVariant("42", "Rouge", PRICE);

        // WHEN
        mongoProductRepository.save(airMax);

        // THEN
        Document stored = mongoTemplate.getCollection("products").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.getList("variants", Document.class)).singleElement().satisfies(storedVariant ->
                assertThat(storedVariant).containsEntry("sku", variant.getSku().toString())
                        .containsEntry("size", "42")
                        .containsEntry("color", "Rouge")
                        .containsEntry("priceAmount", 16_000L)
                        .containsEntry("priceCurrency", "EUR"));
    }

    @Test
    @DisplayName("SHOULD read a product without variants WHEN it was saved before variants existed")
    void shouldReadProductWithoutVariantsWhenSavedBeforeVariantsExisted() {
        // GIVEN
        ProductId id = ProductId.newId();
        mongoTemplate.getCollection("products").insertOne(new Document("_id", id.toString())
                .append("sellerId", "seller-karim")
                .append("name", "Air Max 90 Infrared")
                .append("brand", "Nike")
                .append("category", "sneakers")
                .append("status", "DRAFT")
                .append("createdAt", Date.from(Instant.parse("2026-10-31T10:00:00Z"))));

        // WHEN
        Optional<Product> found = mongoProductRepository.findById(id);

        // THEN
        assertThat(found).get().satisfies(product -> assertThat(product.getVariants()).isEmpty());
    }

    @Test
    @DisplayName("SHOULD keep the existing variants WHEN a variant is added to a product read back from MongoDB")
    void shouldKeepExistingVariantsWhenVariantIsAddedToProductReadBack() {
        // GIVEN
        Product airMax = draftAirMax();
        airMax.addVariant("42", "Rouge", PRICE);
        mongoProductRepository.save(airMax);
        Product readBack = mongoProductRepository.findById(airMax.getId()).orElseThrow();

        // WHEN
        readBack.addVariant("43", "Rouge", PRICE);
        mongoProductRepository.save(readBack);

        // THEN
        Product saved = mongoProductRepository.findById(airMax.getId()).orElseThrow();
        assertThat(saved.getVariants()).extracting(Variant::getSize).containsExactly("42", "43");
    }

    @Test
    @DisplayName("SHOULD refuse the variant WHEN the product read back from MongoDB already has it")
    void shouldRefuseVariantWhenProductReadBackAlreadyHasIt() {
        // GIVEN
        Product airMax = draftAirMax();
        airMax.addVariant("42", "Rouge", PRICE);
        mongoProductRepository.save(airMax);
        Product readBack = mongoProductRepository.findById(airMax.getId()).orElseThrow();

        // WHEN
        Throwable error = catchThrowable(() -> readBack.addVariant("42", "rouge", PRICE));

        // THEN
        assertThat(error).isInstanceOf(DuplicateVariantException.class);
    }

    private static Product draftAirMax() {
        return Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
    }
}
