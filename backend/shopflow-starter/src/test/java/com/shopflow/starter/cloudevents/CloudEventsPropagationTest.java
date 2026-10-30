package com.shopflow.starter.cloudevents;

import com.shopflow.contracts.common.Money;
import com.shopflow.contracts.product.ProductCreated;
import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.contracts.product.ProductVariant;
import com.shopflow.starter.correlation.Correlation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * De bout en bout, avec un vrai Kafka (embarqué) et Avro :
 * « catalog » publie ProductCreated → un consommateur le reçoit et publie ProductPublished en réaction.
 * Vérifie que la corrélation suit le parcours et que la causalité relie les deux messages.
 * Aucune propriété Kafka/Avro n'est déclarée ici : les valeurs par défaut du starter suffisent.
 */
@SpringBootTest(classes = CloudEventsPropagationTest.TestApplication.class, properties = {
        "spring.application.name=catalog-service",
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://cloudevents-propagation"
})
@EmbeddedKafka(partitions = 1, topics = CloudEventsPropagationTest.TOPIC)
class CloudEventsPropagationTest {

    static final String TOPIC = "shopflow.product.events";

    @Autowired
    KafkaEventPublisher publisher;

    @Autowired
    ReactingListener listener;

    @Test
    void propagates_correlation_and_causation_across_messages() throws Exception {
        Correlation.set("corr-journey-42");
        try {
            publisher.publish(TOPIC, "product-1", productCreated()).get(10, TimeUnit.SECONDS);
        } finally {
            Correlation.clear();
        }

        ConsumerRecord<String, Object> created = listener.created.poll(30, TimeUnit.SECONDS);
        ConsumerRecord<String, Object> published = listener.published.poll(30, TimeUnit.SECONDS);
        assertThat(created).isNotNull();
        assertThat(published).isNotNull();

        EventMetadata first = EventMetadata.fromHeaders(created.headers()).orElseThrow();
        EventMetadata second = EventMetadata.fromHeaders(published.headers()).orElseThrow();

        assertThat(first.type()).isEqualTo("shopflow.product.ProductCreated.v1");
        assertThat(first.source()).isEqualTo("/catalog-service");
        assertThat(first.subject()).isEqualTo("product-1").isEqualTo(created.key());
        assertThat(first.correlationId()).isEqualTo("corr-journey-42");
        assertThat(first.causationId()).isNull();

        assertThat(listener.correlationInLogs).as("corrélation visible dans les logs du consommateur")
                .isEqualTo("corr-journey-42");

        assertThat(second.type()).isEqualTo("shopflow.product.ProductPublished.v1");
        assertThat(second.correlationId()).as("même parcours").isEqualTo("corr-journey-42");
        assertThat(second.causationId()).as("provoqué par ProductCreated").isEqualTo(first.id());
        assertThat(published.value()).isInstanceOf(ProductPublished.class);
    }

    private static ProductCreated productCreated() {
        Money price = Money.newBuilder().setAmount(16_000).setCurrency("EUR").build();
        ProductVariant variant = ProductVariant.newBuilder()
                .setSku("AM90-INF-42").setSize("42").setColor("Infrared").setPrice(price).build();
        return ProductCreated.newBuilder()
                .setProductId("product-1")
                .setSellerId("seller-karim")
                .setName("Air Max 90 Infrared")
                .setBrand("Nike")
                .setCategory("sneakers")
                .setVariants(List.of(variant))
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        ReactingListener reactingListener(KafkaEventPublisher publisher) {
            return new ReactingListener(publisher);
        }
    }

    /** Joue le rôle d'un autre service : réagit à ProductCreated en publiant ProductPublished. */
    static class ReactingListener {

        final BlockingQueue<ConsumerRecord<String, Object>> created = new LinkedBlockingQueue<>();
        final BlockingQueue<ConsumerRecord<String, Object>> published = new LinkedBlockingQueue<>();
        volatile String correlationInLogs;

        private final KafkaEventPublisher publisher;

        ReactingListener(KafkaEventPublisher publisher) {
            this.publisher = publisher;
        }

        @KafkaListener(topics = TOPIC, groupId = "propagation-test")
        void on(ConsumerRecord<String, Object> record) {
            if (record.value() instanceof ProductCreated event) {
                correlationInLogs = MDC.get(Correlation.MDC_KEY);
                created.add(record);
                publisher.publish(TOPIC, event.getProductId(), ProductPublished.newBuilder()
                        .setProductId(event.getProductId())
                        .setSellerId(event.getSellerId())
                        .build());
            } else if (record.value() instanceof ProductPublished) {
                published.add(record);
            }
        }
    }
}
