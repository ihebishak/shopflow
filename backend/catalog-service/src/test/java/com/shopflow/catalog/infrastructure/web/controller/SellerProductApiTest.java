package com.shopflow.catalog.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'API vendeur de bout en bout : HTTP → cas d'usage → domaine → vrai MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://seller-product-api"
})
@EmbeddedKafka(partitions = 1)
@Testcontainers
class SellerProductApiTest {

    private static final String PRODUCTS = "/api/v1/seller/products";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("SHOULD create a draft and return its location WHEN a seller creates a product")
    void shouldCreateDraftWhenSellerCreatesProduct() {
        // WHEN
        ResponseEntity<ProductResponseDto> response = createAirMax("seller-karim");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponseDto product = response.getBody();
        assertThat(product).isNotNull();
        assertThat(product.getStatus()).isEqualTo("DRAFT");
        assertThat(product.getSellerId()).isEqualTo("seller-karim");
        assertThat(product.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.getDescription()).isEqualTo("La réédition du modèle de 1990");
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(response.getHeaders().getLocation()).hasPath(PRODUCTS + "/" + product.getId());
    }

    @Test
    @DisplayName("SHOULD return the same product WHEN reading it back from its location")
    void shouldReturnSameProductWhenReadingItBack() {
        // GIVEN
        ResponseEntity<ProductResponseDto> created = createAirMax("seller-karim");

        // WHEN
        ResponseEntity<ProductResponseDto> read = restTemplate.exchange(created.getHeaders().getLocation().getPath(),
                HttpMethod.GET, new HttpEntity<>(sellerHeader("seller-karim")), ProductResponseDto.class);

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read.getBody()).isEqualTo(created.getBody());
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN another seller reads the product")
    void shouldReturn404WhenAnotherSellerReadsProduct() {
        // GIVEN
        URI location = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        ResponseEntity<JsonNode> read = getProblem(location.getPath(), "seller-lea");

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(read.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN the product does not exist")
    void shouldReturn404WhenProductDoesNotExist() {
        // WHEN
        ResponseEntity<JsonNode> read = getProblem(PRODUCTS + "/" + UUID.randomUUID(), "seller-karim");

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(read.getBody().get("correlationId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field WHEN the name is blank")
    void shouldReturn400WhenNameIsBlank() {
        // GIVEN
        CreateProductRequestDto requestDto = CreateProductRequestDto.builder()
                .name(" ").brand("Nike").category("sneakers").build();

        // WHEN
        ResponseEntity<JsonNode> response = restTemplate.exchange(PRODUCTS, HttpMethod.POST,
                new HttpEntity<>(requestDto, sellerHeader("seller-karim")), JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().get("errors").get(0).get("field").asText()).isEqualTo("name");
    }

    private ResponseEntity<ProductResponseDto> createAirMax(final String sellerId) {
        CreateProductRequestDto requestDto = CreateProductRequestDto.builder()
                .name("Air Max 90 Infrared").brand("Nike").category("sneakers")
                .description("La réédition du modèle de 1990").build();
        return restTemplate.exchange(PRODUCTS, HttpMethod.POST, new HttpEntity<>(requestDto, sellerHeader(sellerId)),
                ProductResponseDto.class);
    }

    private ResponseEntity<JsonNode> getProblem(final String path, final String sellerId) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(sellerHeader(sellerId)), JsonNode.class);
    }

    private static HttpHeaders sellerHeader(final String sellerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SellerProductController.SELLER_HEADER, sellerId);
        return headers;
    }
}
