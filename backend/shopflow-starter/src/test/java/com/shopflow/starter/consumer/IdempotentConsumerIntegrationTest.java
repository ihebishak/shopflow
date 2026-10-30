package com.shopflow.starter.consumer;

import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.starter.cloudevents.EventMetadata;
import com.shopflow.starter.cloudevents.EventMetadataFactory;
import com.shopflow.starter.cloudevents.MessageContext;
import com.shopflow.starter.outbox.OutboxPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Consommateurs idempotents de bout en bout : vrai MongoDB (transactions) + vrai Kafka (embarqué) + Avro.
 * Le consommateur de test n'a <b>pas</b> de {@code @Transactional} : le starter ouvre la transaction.
 */
@SpringBootTest(classes = IdempotentConsumerIntegrationTest.TestApplication.class, properties = {
        "spring.application.name=inventory-service",
        "spring.main.web-application-type=none",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://idempotent-consumer",
        "shopflow.consumer.initial-interval=PT0.01S"
})
@EmbeddedKafka(partitions = 1, topics = {IdempotentConsumerIntegrationTest.IN, IdempotentConsumerIntegrationTest.OUT,
        IdempotentConsumerIntegrationTest.DLT})
@Testcontainers
class IdempotentConsumerIntegrationTest {

    static final String IN = "shopflow.product.events";
    static final String OUT = "shopflow.inventory.events";
    static final String DLT = "shopflow.product.events.inventory-service.dlt";
    static final String RESERVATIONS = "reservations";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Value("${spring.embedded.kafka.brokers}")
    String brokers;

    @Autowired
    KafkaTemplate<Object, Object> kafka;

    @Autowired
    EventMetadataFactory metadataFactory;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    StockListener listener;

    @BeforeEach
    void clean() {
        mongoTemplate.remove(new Query(), RESERVATIONS);
        mongoTemplate.remove(new Query(), ProcessedMessage.COLLECTION);
    }

    @Test
    void processes_the_same_message_only_once() throws Exception {
        ProductPublished event = published("dup-1");
        EventMetadata metadata = metadataFactory.create("dup-1", event);
        send(event, metadata);
        send(event, metadata);
        send(published("dup-1-after"), metadataFactory.create("dup-1-after", published("dup-1-after")));

        await(() -> reservations("dup-1-after") == 1);

        assertThat(listener.calls("dup-1")).as("le doublon n'atteint pas le consommateur").isEqualTo(1);
        assertThat(reservations("dup-1")).isEqualTo(1);
        ProcessedMessage marker = mongoTemplate.findById(metadata.getId(), ProcessedMessage.class);
        assertThat(marker).isNotNull();
        assertThat(marker.getType()).isEqualTo("shopflow.product.ProductPublished.v1");
        assertThat(marker.getTopic()).isEqualTo(IN);
    }

    @Test
    void a_failed_attempt_leaves_nothing_behind_and_the_retry_processes_the_message() throws Exception {
        EventMetadata metadata = send(published("flaky-1"));

        await(() -> mongoTemplate.findById(metadata.getId(), ProcessedMessage.class) != null);

        assertThat(listener.calls("flaky-1")).as("échec puis réussite").isEqualTo(2);
        assertThat(reservations("flaky-1")).as("la réservation du 1er essai est annulée").isEqualTo(1);
    }

    @Test
    void publishes_through_the_outbox_without_transactional_and_publishes_nothing_on_failure() throws Exception {
        EventMetadata ok = send(published("outbox-ok-1"));
        send(published("outbox-fail-1"));

        ConsumerRecord<String, Object> reaction = listener.reactions.poll(30, TimeUnit.SECONDS);
        assertThat(reaction).isNotNull();
        assertThat(reaction.key()).isEqualTo("outbox-ok-1");
        assertThat(EventMetadata.fromHeaders(reaction.headers()).orElseThrow().getCausationId())
                .as("provoqué par le message reçu").isEqualTo(ok.getId());

        DeadLetters.await(brokers, DLT, "outbox-fail-1");
        assertThat(reservations("outbox-fail-1")).isZero();
        assertThat(mongoTemplate.count(query(where("key").is("outbox-fail-1")), "outbox"))
                .as("rien dans l'outbox").isZero();
        assertThat(listener.reactions.poll(1, TimeUnit.SECONDS)).as("rien publié").isNull();
    }

    @Test
    void ignores_a_message_another_instance_processed_at_the_same_time() throws Exception {
        EventMetadata metadata = send(published("concurrent-1"));
        send(published("after-concurrent-1"));

        await(() -> reservations("after-concurrent-1") == 1);

        assertThat(listener.calls("concurrent-1")).as("essai annulé (conflit), puis réessai reconnu comme doublon")
                .isEqualTo(1);
        assertThat(reservations("concurrent-1")).as("la réservation de cette instance est annulée").isZero();
        assertThat(mongoTemplate.findById(metadata.getId(), Document.class, ProcessedMessage.COLLECTION))
                .containsEntry("by", "other-instance");
    }

    private EventMetadata send(ProductPublished event) throws Exception {
        EventMetadata metadata = metadataFactory.create(event.getProductId(), event);
        send(event, metadata);
        return metadata;
    }

    private void send(ProductPublished event, EventMetadata metadata) throws Exception {
        kafka.send(new ProducerRecord<>(IN, null, event.getProductId(), event, metadata.toHeaders()))
                .get(10, TimeUnit.SECONDS);
    }

    private long reservations(String productId) {
        return mongoTemplate.count(query(where("productId").is(productId)), RESERVATIONS);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 300 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).as("condition reached within 30 s").isTrue();
    }

    private static ProductPublished published(String productId) {
        return ProductPublished.newBuilder().setProductId(productId).setSellerId("seller-karim").build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        StockListener stockListener(MongoTemplate mongoTemplate, OutboxPublisher outbox) {
            return new StockListener(mongoTemplate, outbox);
        }
    }

    /**
     * Joue le rôle d'inventory-service : réserve du stock (écriture MongoDB) pour chaque produit publié.
     * Le comportement dépend du préfixe de l'identifiant du produit.
     */
    static class StockListener {

        private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
        final BlockingQueue<ConsumerRecord<String, Object>> reactions = new LinkedBlockingQueue<>();
        private final MongoTemplate mongoTemplate;
        private final OutboxPublisher outbox;

        StockListener(MongoTemplate mongoTemplate, OutboxPublisher outbox) {
            this.mongoTemplate = mongoTemplate;
            this.outbox = outbox;
        }

        int calls(String productId) {
            return calls.getOrDefault(productId, new AtomicInteger()).get();
        }

        @KafkaListener(topics = IN)
        void on(ProductPublished event) {
            String productId = event.getProductId();
            int call = calls.computeIfAbsent(productId, id -> new AtomicInteger()).incrementAndGet();
            mongoTemplate.insert(new Document("productId", productId), RESERVATIONS);

            if (productId.startsWith("flaky") && call == 1) {
                throw new IllegalStateException("MongoDB hiccup");
            }
            if (productId.startsWith("outbox")) {
                outbox.publish(OUT, productId, ProductPublished.newBuilder()
                        .setProductId(productId).setSellerId("inventory").build());
                if (productId.startsWith("outbox-fail")) {
                    throw new IllegalStateException("boom");
                }
            }
            if (productId.startsWith("concurrent") && call == 1) {
                // Une autre instance valide le même message pendant ce traitement (hors de notre transaction)
                String id = MessageContext.current().orElseThrow().getId();
                CompletableFuture.runAsync(() -> mongoTemplate.insert(
                        new Document("_id", id).append("by", "other-instance"), ProcessedMessage.COLLECTION)).join();
            }
        }

        @KafkaListener(topics = OUT, groupId = "reactions-collector")
        void collect(ConsumerRecord<String, Object> record) {
            reactions.add(record);
        }
    }
}
