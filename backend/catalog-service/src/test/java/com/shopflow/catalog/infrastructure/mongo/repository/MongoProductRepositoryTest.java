package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapperImpl;
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

/**
 * L'adaptateur avec un vrai MongoDB : un produit enregistré puis relu est identique.
 */
@Testcontainers
class MongoProductRepositoryTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

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
}
