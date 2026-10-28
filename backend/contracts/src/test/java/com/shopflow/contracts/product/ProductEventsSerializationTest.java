package com.shopflow.contracts.product;

import com.shopflow.contracts.common.Money;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que chaque événement produit passe par le Schema Registry (simulé) et revient identique,
 * et que la stratégie TopicRecordNameStrategy enregistre un schéma par type d'événement du topic.
 */
class ProductEventsSerializationTest {

    private static final String TOPIC = "shopflow.product.events";
    private static final String SCOPE = "product-contracts-test";

    private KafkaAvroSerializer serializer;
    private KafkaAvroDeserializer deserializer;

    @BeforeEach
    void setUp() {
        Map<String, Object> config = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://" + SCOPE,
                AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName(),
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        serializer = new KafkaAvroSerializer();
        serializer.configure(config, false);
        deserializer = new KafkaAvroDeserializer();
        deserializer.configure(config, false);
    }

    @AfterEach
    void tearDown() {
        MockSchemaRegistry.dropScope(SCOPE);
    }

    static Stream<SpecificRecord> productEvents() {
        ProductVariant variant42 = ProductVariant.newBuilder()
                .setSku("AM90-INF-42")
                .setSize("42")
                .setColor("Infrared")
                .setPrice(euros(16_000))
                .build();
        return Stream.of(
                ProductCreated.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setSellerId("seller-karim")
                        .setName("Air Max 90 Infrared")
                        .setBrand("Nike")
                        .setCategory("sneakers")
                        .setVariants(List.of(variant42))
                        .build(),
                ProductVariantAdded.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setVariant(variant42)
                        .build(),
                ProductUpdated.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setChangedFields(List.of("description", "images"))
                        .build(),
                ProductPriceChanged.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setSku("AM90-INF-42")
                        .setOldPrice(euros(16_000))
                        .setNewPrice(euros(12_900))
                        .build(),
                ProductPublished.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setSellerId("seller-karim")
                        .build(),
                ProductUnpublished.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setSellerId("seller-karim")
                        .setReason(UnpublishReason.MODERATION)
                        .build(),
                ProductReported.newBuilder()
                        .setProductId("0192f0c4-8a1e-7c3b-9d2f-5e6a7b8c9d0e")
                        .setSellerId("seller-karim")
                        .setReason("Contrefaçon suspectée")
                        .build());
    }

    @ParameterizedTest
    @MethodSource("productEvents")
    void event_survives_a_round_trip_through_the_schema_registry(SpecificRecord event) {
        byte[] bytes = serializer.serialize(TOPIC, event);

        Object result = deserializer.deserialize(TOPIC, bytes);

        assertThat(result).isInstanceOf(event.getClass()).isEqualTo(event);
    }

    @Test
    void registers_one_subject_per_event_type_of_the_topic() throws Exception {
        productEvents().forEach(event -> serializer.serialize(TOPIC, event));

        assertThat(MockSchemaRegistry.getClientForScope(SCOPE).getAllSubjects()).containsExactlyInAnyOrder(
                TOPIC + "-com.shopflow.contracts.product.ProductCreated",
                TOPIC + "-com.shopflow.contracts.product.ProductVariantAdded",
                TOPIC + "-com.shopflow.contracts.product.ProductUpdated",
                TOPIC + "-com.shopflow.contracts.product.ProductPriceChanged",
                TOPIC + "-com.shopflow.contracts.product.ProductPublished",
                TOPIC + "-com.shopflow.contracts.product.ProductUnpublished",
                TOPIC + "-com.shopflow.contracts.product.ProductReported");
    }

    private static Money euros(long cents) {
        return Money.newBuilder().setAmount(cents).setCurrency("EUR").build();
    }
}
