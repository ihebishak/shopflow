package com.shopflow.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopflow.starter.consumer.IdempotentListenerAdvice;
import com.shopflow.starter.outbox.OutboxPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le service démarre avec un vrai MongoDB et un vrai Kafka (embarqué),
 * et le socle du starter se branche tout seul, sans aucune configuration dans le service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://catalog-service"
})
@EmbeddedKafka(partitions = 1)
@Testcontainers
class CatalogServiceApplicationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate http;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("SHOULD report itself healthy with MongoDB WHEN the service is running")
    void shouldReportHealthyWhenServiceIsRunning() {
        // WHEN
        Map<String, Object> health = http.getForObject("/actuator/health", Map.class);

        // THEN
        assertThat(health).containsEntry("status", "UP");
        assertThat((Map<String, Object>) health.get("components")).containsKey("mongo");
    }

    @Test
    @DisplayName("SHOULD get the starter foundations WHEN the service declares no configuration for them")
    void shouldGetStarterFoundationsWithoutConfiguration() {
        // WHEN / THEN
        assertThat(context.getBean(OutboxPublisher.class)).as("outbox").isNotNull();
        assertThat(context.getBean(IdempotentListenerAdvice.class)).as("consommateurs idempotents").isNotNull();
        assertThat(context.getBean(CommonErrorHandler.class)).as("réessais puis DLT")
                .isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    @DisplayName("SHOULD publish the OpenAPI documentation of the seller product api WHEN the service is running")
    void shouldPublishOpenApiDocumentationWhenServiceIsRunning() {
        // WHEN
        String apiDocs = http.getForObject("/v3/api-docs", String.class);

        // THEN
        assertThat(apiDocs)
                .contains("ShopFlow — Catalogue")
                .contains("\"/api/v1/seller/products\"")
                .contains("\"/api/v1/seller/products/{productId}\"")
                .contains("Produits (vendeur)")
                .contains("X-Seller-Id");
        assertThat(http.getForEntity("/swagger-ui/index.html", String.class).getStatusCode().is2xxSuccessful())
                .as("page Swagger UI").isTrue();
    }

    @Test
    @DisplayName("SHOULD show the description, the example and the required fields of a DTO WHEN its interface documents them")
    void shouldShowDtoDocumentationWhenItsInterfaceDocumentsIt() {
        // WHEN
        JsonNode schemas = http.getForObject("/v3/api-docs", JsonNode.class).at("/components/schemas");

        // THEN
        JsonNode request = schemas.get("UpdateProductRequestDto");
        assertThat(request.get("description").isTextual()).as("description de la classe").isTrue();
        assertThat(request.at("/properties/name/description").asText()).isEqualTo("Nom du produit.");
        assertThat(request.at("/properties/name/example").asText()).isEqualTo("Air Max 90 Infrared");
        assertThat(request.at("/properties/name/maxLength").asInt()).as("Bean Validation du DTO").isEqualTo(120);
        assertThat(request.get("required")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("name", "brand", "category");
        JsonNode response = schemas.get("VariantResponseDto");
        assertThat(response.at("/properties/size/description").asText()).isEqualTo("Taille.");
        assertThat(response.at("/properties/price/description").asText()).isEqualTo("Prix de la variante.");
    }
}
