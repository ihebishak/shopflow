package com.shopflow.starter.outbox;

import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.starter.cloudevents.EventMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outbox de bout en bout : vrai MongoDB (replica set, transactions) + vrai Kafka (embarqué) + Avro.
 * Le filet de sécurité est réglé à 1 minute : seul le réveil après commit peut publier rapidement.
 * Aucune propriété Kafka/Avro n'est déclarée ici : les valeurs par défaut du starter suffisent.
 */
@SpringBootTest(classes = OutboxIntegrationTest.TestApplication.class, properties = {
        "spring.application.name=catalog-service",
        "spring.main.web-application-type=none",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://outbox-integration",
        "shopflow.outbox.poll-interval=PT1M"
})
@EmbeddedKafka(partitions = 1, topics = OutboxIntegrationTest.TOPIC)
@Testcontainers
class OutboxIntegrationTest {

    static final String TOPIC = "shopflow.product.events";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    OutboxPublisher outbox;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    Collector collector;

    @BeforeEach
    void clean() {
        mongoTemplate.remove(new Query(), OutboxMessage.COLLECTION);
        collector.records.clear();
    }

    @Test
    void publishes_the_event_on_kafka_as_soon_as_the_transaction_commits() throws Exception {
        transaction.executeWithoutResult(status -> outbox.publish(TOPIC, "product-1", published("seller-1")));

        ConsumerRecord<String, Object> record = collector.records.poll(10, TimeUnit.SECONDS);
        assertThat(record).as("réveillé après commit, sans attendre le filet de sécurité").isNotNull();
        assertThat(record.key()).isEqualTo("product-1");
        assertThat(record.value()).isEqualTo(published("seller-1"));
        EventMetadata metadata = EventMetadata.fromHeaders(record.headers()).orElseThrow();
        assertThat(metadata.type()).isEqualTo("shopflow.product.ProductPublished.v1");
        assertThat(metadata.source()).isEqualTo("/catalog-service");

        Document stored = waitUntilPublished(metadata.id());
        assertThat(stored.getString("payload")).as("lisible dans Mongo Express")
                .isEqualTo("{\"productId\":\"product-1\",\"sellerId\":\"seller-1\"}");
    }

    @Test
    void publishes_nothing_when_the_transaction_rolls_back() throws Exception {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            outbox.publish(TOPIC, "product-1", published("seller-1"));
            throw new IllegalStateException("business rule violated");
        })).hasMessage("business rule violated");

        assertThat(mongoTemplate.count(new Query(), OutboxMessage.COLLECTION)).isZero();
        assertThat(collector.records.poll(3, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void refuses_to_publish_outside_a_transaction() {
        assertThatThrownBy(() -> outbox.publish(TOPIC, "product-1", published("seller-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@Transactional");
    }

    @Test
    void keeps_the_order_of_events_written_in_the_same_transaction() throws Exception {
        transaction.executeWithoutResult(status -> {
            for (int i = 0; i < 20; i++) {
                outbox.publish(TOPIC, "product-1", published("seller-" + i));
            }
        });

        List<String> received = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ConsumerRecord<String, Object> record = collector.records.poll(10, TimeUnit.SECONDS);
            assertThat(record).isNotNull();
            received.add(((ProductPublished) record.value()).getSellerId());
        }
        assertThat(received).containsExactly(
                "seller-0", "seller-1", "seller-2", "seller-3", "seller-4", "seller-5", "seller-6",
                "seller-7", "seller-8", "seller-9", "seller-10", "seller-11", "seller-12", "seller-13",
                "seller-14", "seller-15", "seller-16", "seller-17", "seller-18", "seller-19");
    }

    private Document waitUntilPublished(String id) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Document stored = mongoTemplate.getCollection(OutboxMessage.COLLECTION)
                    .find(new Document("_id", id)).first();
            if (stored != null && stored.get("publishedAt") != null) {
                return stored;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Outbox message " + id + " was never marked as published");
    }

    private static ProductPublished published(String sellerId) {
        return ProductPublished.newBuilder().setProductId("product-1").setSellerId(sellerId).build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        Collector collector() {
            return new Collector();
        }
    }

    static class Collector {

        final BlockingQueue<ConsumerRecord<String, Object>> records = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC, groupId = "outbox-integration-test")
        void on(ConsumerRecord<String, Object> record) {
            records.add(record);
        }
    }
}
