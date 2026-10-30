package com.shopflow.catalog.infrastructure.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'adaptateur avec un vrai MongoDB : un produit enregistré puis relu est identique.
 */
@Testcontainers
class MongoProductRepositoryTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static MongoClient client;
    private MongoTemplate mongoTemplate;
    private MongoProductRepository repository;

    @BeforeAll
    static void connect() {
        client = MongoClients.create(MONGO.getReplicaSetUrl());
    }

    @AfterAll
    static void disconnect() {
        client.close();
    }

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate = new MongoTemplate(client, "catalog-test");
        mongoTemplate.remove(new Query(), ProductDocument.COLLECTION);
        repository = new MongoProductRepository(mongoTemplate);
    }

    @Test
    void reads_back_exactly_the_product_it_saved() {
        Product airMax = Product.draft(new SellerId("seller-karim"), "Air Max 90 Infrared", "Nike", "sneakers",
                "La réédition du modèle de 1990", Instant.now());

        repository.save(airMax);

        assertThat(repository.findById(airMax.id())).get().usingRecursiveComparison().isEqualTo(airMax);
    }

    @Test
    void stores_a_readable_document_in_the_products_collection() {
        Product airMax = Product.draft(new SellerId("seller-karim"), "Air Max 90 Infrared", "Nike", "sneakers",
                null, Instant.parse("2026-10-31T10:00:00Z"));

        repository.save(airMax);

        Document stored = mongoTemplate.getCollection("products").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.get("_id")).isEqualTo(airMax.id().toString());
        assertThat(stored).containsEntry("sellerId", "seller-karim")
                .containsEntry("name", "Air Max 90 Infrared")
                .containsEntry("brand", "Nike")
                .containsEntry("category", "sneakers")
                .containsEntry("status", "DRAFT")
                .containsEntry("createdAt", Date.from(Instant.parse("2026-10-31T10:00:00Z")))
                .doesNotContainKey("description");
    }

    @Test
    void finds_nothing_for_an_unknown_id() {
        assertThat(repository.findById(ProductId.newId())).isEmpty();
    }
}
