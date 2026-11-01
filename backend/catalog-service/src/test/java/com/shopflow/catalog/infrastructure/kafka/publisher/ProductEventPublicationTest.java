package com.shopflow.catalog.infrastructure.kafka.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.web.controller.SellerProductController;
import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.MoneyRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductVariant;
import com.shopflow.contracts.product.ProductVariantAdded;
import com.shopflow.starter.cloudevents.EventMetadata;
import com.shopflow.starter.outbox.OutboxPublisher;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * De bout en bout : HTTP → cas d'usage → MongoDB + outbox → relais → vrai Kafka (embarqué), avec Avro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=" + ProductEventPublicationTest.SCHEMA_REGISTRY
})
@EmbeddedKafka(partitions = 1, topics = OutboxProductEventPublisher.TOPIC)
@Testcontainers
class ProductEventPublicationTest {

    static final String SCHEMA_REGISTRY = "mock://product-event-publication";
    private static final String PRODUCTS = "/api/v1/seller/products";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoSpyBean
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("SHOULD publish ProductCreated on Kafka with its envelope WHEN a seller creates a product")
    void shouldPublishProductCreatedWhenSellerCreatesProduct() {
        // GIVEN
        String correlationId = "corr-" + UUID.randomUUID();

        // WHEN
        ResponseEntity<ProductResponseDto> response = restTemplate.exchange(PRODUCTS, HttpMethod.POST,
                new HttpEntity<>(airMax("Air Max 90 Infrared"), headers(correlationId)), ProductResponseDto.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String productId = response.getBody().getId();
        ConsumerRecord<String, SpecificRecord> record = awaitEvent(productId, ProductCreated.class);

        ProductCreated event = (ProductCreated) record.value();
        assertThat(event.getProductId()).isEqualTo(productId);
        assertThat(event.getSellerId()).isEqualTo("seller-karim");
        assertThat(event.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(event.getBrand()).isEqualTo("Nike");
        assertThat(event.getCategory()).isEqualTo("sneakers");
        assertThat(event.getVariants()).isEmpty();

        EventMetadata metadata = EventMetadata.fromHeaders(record.headers()).orElseThrow();
        assertThat(metadata.getType()).isEqualTo("shopflow.product.ProductCreated.v1");
        assertThat(metadata.getSource()).isEqualTo("/catalog-service");
        assertThat(metadata.getSubject()).isEqualTo(productId);
        assertThat(metadata.getCorrelationId()).as("même parcours que la requête HTTP").isEqualTo(correlationId);
    }

    @Test
    @DisplayName("SHOULD save nothing WHEN the event cannot be written to the outbox")
    void shouldSaveNothingWhenEventCannotBeWritten() {
        // GIVEN
        String name = "Rollback " + UUID.randomUUID();
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outboxPublisher).publish(anyString(), anyString(), any(SpecificRecord.class));

        // WHEN
        ResponseEntity<JsonNode> response = restTemplate.exchange(PRODUCTS, HttpMethod.POST,
                new HttpEntity<>(airMax(name), headers("corr-rollback")), JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(mongoTemplate.count(query(where("name").is(name)), ProductDocument.COLLECTION))
                .as("le produit est annulé avec l'événement").isZero();
    }

    @Test
    @DisplayName("SHOULD publish ProductVariantAdded on Kafka with its envelope WHEN a seller adds a variant")
    void shouldPublishProductVariantAddedWhenSellerAddsVariant() {
        // GIVEN
        String productId = createdProductId();
        String correlationId = "corr-" + UUID.randomUUID();

        // WHEN
        ResponseEntity<VariantResponseDto> response = restTemplate.exchange(variantsOf(productId), HttpMethod.POST,
                new HttpEntity<>(variant42Rouge(), headers(correlationId)), VariantResponseDto.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ConsumerRecord<String, SpecificRecord> record = awaitEvent(productId, ProductVariantAdded.class);

        ProductVariantAdded event = (ProductVariantAdded) record.value();
        assertThat(event.getProductId()).isEqualTo(productId);
        ProductVariant variant = event.getVariant();
        assertThat(variant.getSku()).isEqualTo(response.getBody().getSku());
        assertThat(variant.getSize()).isEqualTo("42");
        assertThat(variant.getColor()).isEqualTo("Rouge");
        assertThat(variant.getPrice().getAmount()).isEqualTo(16_000);
        assertThat(variant.getPrice().getCurrency()).isEqualTo("EUR");

        EventMetadata metadata = EventMetadata.fromHeaders(record.headers()).orElseThrow();
        assertThat(metadata.getType()).isEqualTo("shopflow.product.ProductVariantAdded.v1");
        assertThat(metadata.getSource()).isEqualTo("/catalog-service");
        assertThat(metadata.getSubject()).isEqualTo(productId);
        assertThat(metadata.getCorrelationId()).as("même parcours que la requête HTTP").isEqualTo(correlationId);
    }

    @Test
    @DisplayName("SHOULD not add the variant WHEN the event cannot be written to the outbox")
    void shouldNotAddVariantWhenEventCannotBeWritten() {
        // GIVEN
        String productId = createdProductId();
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outboxPublisher).publish(anyString(), anyString(), any(SpecificRecord.class));

        // WHEN
        ResponseEntity<JsonNode> response = restTemplate.exchange(variantsOf(productId), HttpMethod.POST,
                new HttpEntity<>(variant42Rouge(), headers("corr-rollback")), JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("code").asText()).isEqualTo("INTERNAL_ERROR");
        ProductResponseDto product = restTemplate.exchange(PRODUCTS + "/" + productId, HttpMethod.GET,
                new HttpEntity<>(headers("corr-read")), ProductResponseDto.class).getBody();
        assertThat(product.getVariants()).as("la variante est annulée avec l'événement").isEmpty();
    }

    private String createdProductId() {
        return restTemplate.exchange(PRODUCTS, HttpMethod.POST,
                new HttpEntity<>(airMax("Air Max 90 Infrared"), headers("corr-create")), ProductResponseDto.class)
                .getBody().getId();
    }

    private static String variantsOf(final String productId) {
        return PRODUCTS + "/" + productId + "/variants";
    }

    private static AddVariantRequestDto variant42Rouge() {
        return AddVariantRequestDto.builder().size("42").color("Rouge")
                .price(MoneyRequestDto.builder().amount(16_000L).currency("EUR").build()).build();
    }

    private static CreateProductRequestDto airMax(final String name) {
        return CreateProductRequestDto.builder().name(name).brand("Nike").category("sneakers").build();
    }

    private static HttpHeaders headers(final String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SellerProductController.SELLER_HEADER, "seller-karim");
        headers.set("X-Correlation-Id", correlationId);
        return headers;
    }

    private ConsumerRecord<String, SpecificRecord> awaitEvent(final String key,
                                                              final Class<? extends SpecificRecord> eventType) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ConsumerConfig.GROUP_ID_CONFIG, "publication-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class,
                "schema.registry.url", SCHEMA_REGISTRY,
                "specific.avro.reader", true);
        try (Consumer<String, SpecificRecord> consumer =
                     new DefaultKafkaConsumerFactory<String, SpecificRecord>(config).createConsumer()) {
            consumer.subscribe(List.of(OutboxProductEventPublisher.TOPIC));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, SpecificRecord> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key()) && eventType.isInstance(record.value())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("No " + eventType.getSimpleName() + " with key " + key + " on "
                + OutboxProductEventPublisher.TOPIC);
    }
}
