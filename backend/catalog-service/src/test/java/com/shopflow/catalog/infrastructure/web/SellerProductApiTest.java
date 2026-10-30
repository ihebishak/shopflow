package com.shopflow.catalog.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    TestRestTemplate http;

    @Test
    void creates_a_draft_and_returns_its_location() {
        ResponseEntity<ProductResponse> response = create("seller-karim");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponse product = response.getBody();
        assertThat(product).isNotNull();
        assertThat(product.status()).isEqualTo("DRAFT");
        assertThat(product.sellerId()).isEqualTo("seller-karim");
        assertThat(product.name()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.description()).isEqualTo("La réédition du modèle de 1990");
        assertThat(product.createdAt()).isNotNull();
        assertThat(response.getHeaders().getLocation()).hasPath(PRODUCTS + "/" + product.id());
    }

    @Test
    void reads_back_the_created_product() {
        ResponseEntity<ProductResponse> created = create("seller-karim");

        ResponseEntity<ProductResponse> read = get(created.getHeaders().getLocation(), "seller-karim");

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read.getBody()).isEqualTo(created.getBody());
    }

    @Test
    void hides_a_product_from_other_sellers() {
        ResponseEntity<ProductResponse> created = create("seller-karim");

        ResponseEntity<ProductResponse> read = get(created.getHeaders().getLocation(), "seller-lea");

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void answers_404_for_an_unknown_product() {
        ResponseEntity<ProductResponse> read = get(URI.create(PRODUCTS + "/" + UUID.randomUUID()), "seller-karim");

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requires_the_seller_header() {
        ResponseEntity<String> response = http.postForEntity(PRODUCTS,
                new CreateProductRequest("Air Max 90 Infrared", "Nike", "sneakers", null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ProductResponse> create(String sellerId) {
        CreateProductRequest request = new CreateProductRequest("Air Max 90 Infrared", "Nike", "sneakers",
                "La réédition du modèle de 1990");
        return http.exchange(PRODUCTS, HttpMethod.POST, new HttpEntity<>(request, seller(sellerId)),
                ProductResponse.class);
    }

    private ResponseEntity<ProductResponse> get(URI location, String sellerId) {
        return http.exchange(location.getPath(), HttpMethod.GET, new HttpEntity<>(seller(sellerId)),
                ProductResponse.class);
    }

    private static HttpHeaders seller(String sellerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SellerProductController.SELLER_HEADER, sellerId);
        return headers;
    }
}
