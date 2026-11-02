package com.shopflow.catalog.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateCategoryRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'API admin des catégories de bout en bout : HTTP → cas d'usage → domaine → vrai MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://admin-category-api"
})
@EmbeddedKafka(partitions = 1)
@Testcontainers
class AdminCategoryApiTest {

    private static final String CATEGORIES = "/api/v1/admin/categories";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("SHOULD build the tree down to the third level and refuse a fourth WHEN categories are created")
    void shouldBuildTreeAndRefuseFourthLevelWhenCategoriesAreCreated() {
        // GIVEN
        String shoes = unique("chaussures");
        String sneakers = unique("sneakers");
        String running = unique("running");

        // WHEN
        ResponseEntity<CategoryResponseDto> first = create(shoes, "Chaussures", null);
        ResponseEntity<CategoryResponseDto> second = create(sneakers, "Sneakers", shoes);
        ResponseEntity<CategoryResponseDto> third = create(running, "Running", sneakers);
        ResponseEntity<JsonNode> fourth = createProblem(unique("trail"), "Trail", running);

        // THEN
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().getAncestors()).isEmpty();
        assertThat(second.getBody().getAncestors()).containsExactly(shoes);
        assertThat(third.getBody().getSlug()).isEqualTo(running);
        assertThat(third.getBody().getName()).isEqualTo("Running");
        assertThat(third.getBody().getAncestors()).containsExactly(shoes, sneakers);
        assertThat(fourth.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(fourth.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(fourth.getBody().get("code").asText()).isEqualTo("CATEGORY_TOO_DEEP");
    }

    @Test
    @DisplayName("SHOULD return 404 CATEGORY_NOT_FOUND WHEN the parent does not exist")
    void shouldReturn404WhenParentDoesNotExist() {
        // WHEN
        ResponseEntity<JsonNode> response = createProblem(unique("sneakers"), "Sneakers", unique("inconnue"));

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("CATEGORY_NOT_FOUND");
    }

    @Test
    @DisplayName("SHOULD return 409 CATEGORY_ALREADY_EXISTS WHEN the slug is already taken")
    void shouldReturn409WhenSlugIsTaken() {
        // GIVEN
        String shoes = unique("chaussures");
        create(shoes, "Chaussures", null);

        // WHEN
        ResponseEntity<JsonNode> response = createProblem(shoes, "Souliers", null);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code").asText()).isEqualTo("CATEGORY_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("SHOULD return 400 INVALID_CATEGORY_SLUG WHEN the slug is badly formed")
    void shouldReturn400WhenSlugIsBadlyFormed() {
        // WHEN
        ResponseEntity<JsonNode> response = createProblem("Sneakers", "Sneakers", null);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("INVALID_CATEGORY_SLUG");
    }

    @Test
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field WHEN the name is blank")
    void shouldReturn400WithFieldWhenNameIsBlank() {
        // WHEN
        ResponseEntity<JsonNode> response = createProblem(unique("sneakers"), " ", null);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().at("/errors/0/field").asText()).isEqualTo("name");
    }

    /** MongoDB est partagé entre les tests de la classe : chacun crée ses propres identifiants. */
    private static String unique(final String slug) {
        return slug + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private ResponseEntity<CategoryResponseDto> create(final String slug, final String name, final String parentSlug) {
        return restTemplate.postForEntity(CATEGORIES, request(slug, name, parentSlug), CategoryResponseDto.class);
    }

    private ResponseEntity<JsonNode> createProblem(final String slug, final String name, final String parentSlug) {
        return restTemplate.postForEntity(CATEGORIES, request(slug, name, parentSlug), JsonNode.class);
    }

    private static CreateCategoryRequestDto request(final String slug, final String name, final String parentSlug) {
        return CreateCategoryRequestDto.builder().slug(slug).name(name).parentSlug(parentSlug).build();
    }
}
