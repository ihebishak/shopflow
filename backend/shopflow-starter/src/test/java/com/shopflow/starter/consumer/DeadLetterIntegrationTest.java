package com.shopflow.starter.consumer;

import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.starter.cloudevents.KafkaEventPublisher;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.shopflow.starter.consumer.DeadLetters.header;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Réessais sur place puis DLT, avec un vrai Kafka (embarqué) et Avro, dans un service <b>sans MongoDB</b> :
 * la politique d'erreur s'applique à tous les services.
 */
@SpringBootTest(classes = DeadLetterIntegrationTest.TestApplication.class, properties = {
        "spring.application.name=catalog-service",
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://dead-letter",
        "shopflow.consumer.initial-interval=PT0.01S"
})
@EmbeddedKafka(partitions = 1, topics = {DeadLetterIntegrationTest.TOPIC, DeadLetterIntegrationTest.DLT})
class DeadLetterIntegrationTest {

    static final String TOPIC = "shopflow.product.events";
    static final String DLT = "shopflow.product.events.catalog-service.dlt";

    @Value("${spring.embedded.kafka.brokers}")
    String brokers;

    @Autowired
    KafkaEventPublisher publisher;

    @Autowired
    FailingListener listener;

    @Test
    void retries_four_times_then_moves_the_message_to_the_service_dead_letter_topic() throws Exception {
        publisher.publish(TOPIC, "fail-1", published("fail-1")).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, byte[]> dead = DeadLetters.await(brokers, DLT, "fail-1");

        assertThat(listener.handled.stream().filter("fail-1"::equals).toList()).as("1 essai + 4 réessais")
                .hasSize(5);
        assertThat(header(dead, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains("boom: fail-1");
        assertThat(header(dead, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
        assertThat(header(dead, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP)).isEqualTo("catalog-service");
        assertThat(header(dead, "ce_type")).as("enveloppe CloudEvents conservée")
                .isEqualTo("shopflow.product.ProductPublished.v1");
        assertThat(decode(dead.value())).as("contenu d'origine intact").isEqualTo(published("fail-1"));
    }

    @Test
    void keeps_the_order_the_next_message_waits_until_the_failing_one_is_set_aside() throws Exception {
        publisher.publish(TOPIC, "fail-2", published("fail-2"));
        publisher.publish(TOPIC, "ok-2", published("ok-2")).get(10, TimeUnit.SECONDS);

        awaitHandled("ok-2");

        assertThat(listener.handled.stream().filter(id -> id.endsWith("-2")).toList()).containsExactly(
                "fail-2", "fail-2", "fail-2", "fail-2", "fail-2", "ok-2");
    }

    @Test
    void sends_an_unreadable_message_straight_to_the_dead_letter_topic_without_blocking() throws Exception {
        byte[] garbage = "definitely not avro".getBytes(StandardCharsets.UTF_8);
        rawKafka().send(TOPIC, "poison", garbage).get(10, TimeUnit.SECONDS);
        publisher.publish(TOPIC, "ok-3", published("ok-3")).get(10, TimeUnit.SECONDS);

        awaitHandled("ok-3");
        ConsumerRecord<String, byte[]> dead = DeadLetters.await(brokers, DLT, "poison");

        assertThat(listener.handled).as("jamais transmis au consommateur, aucun réessai").doesNotContain("poison");
        assertThat(dead.value()).as("octets bruts d'origine").isEqualTo(garbage);
        assertThat(header(dead, KafkaHeaders.DLT_EXCEPTION_FQCN)).contains("DeserializationException");
    }

    private void awaitHandled(String productId) throws InterruptedException {
        for (int i = 0; i < 300 && !listener.handled.contains(productId); i++) {
            Thread.sleep(100);
        }
        assertThat(listener.handled).contains(productId);
    }

    private KafkaTemplate<String, byte[]> rawKafka() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers),
                new StringSerializer(), new ByteArraySerializer()));
    }

    private static Object decode(byte[] value) {
        try (KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer()) {
            deserializer.configure(Map.of("schema.registry.url", "mock://dead-letter", "specific.avro.reader", true),
                    false);
            return deserializer.deserialize(DLT, value);
        }
    }

    private static ProductPublished published(String productId) {
        return ProductPublished.newBuilder().setProductId(productId).setSellerId("seller-karim").build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        FailingListener failingListener() {
            return new FailingListener();
        }
    }

    /** Échoue toujours sur les produits « fail-* » ; note chaque appel, dans l'ordre. */
    static class FailingListener {

        final List<String> handled = new CopyOnWriteArrayList<>();

        @KafkaListener(topics = TOPIC)
        void on(ProductPublished event) {
            handled.add(event.getProductId());
            if (event.getProductId().startsWith("fail")) {
                throw new IllegalStateException("boom: " + event.getProductId());
            }
        }
    }
}
