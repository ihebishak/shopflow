package com.shopflow.starter.outbox;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.starter.cloudevents.EventMetadataFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Comportement du relais face aux pannes, avec un vrai MongoDB et un Kafka simulé.
 */
@Testcontainers
class OutboxRelayTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static MongoClient client;
    private MongoTemplate mongoTemplate;
    private final EventMetadataFactory metadataFactory = new EventMetadataFactory("order-service", Clock.systemUTC());
    private final OutboxProperties properties =
            new OutboxProperties(Duration.ofSeconds(1), 100, Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofDays(7));

    @BeforeAll
    static void connect() {
        client = MongoClients.create(MONGO.getReplicaSetUrl());
    }

    @AfterAll
    static void disconnect() {
        client.close();
    }

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate = new MongoTemplate(client, "relay-test");
        mongoTemplate.remove(new Query(), OutboxMessage.COLLECTION);
        mongoTemplate.remove(new Query(), OutboxLock.COLLECTION);
    }

    @Test
    void stops_at_the_first_failure_then_resumes_in_order() {
        OutboxMessage first = store("seller-1");
        OutboxMessage second = store("seller-2");
        OutboxMessage third = store("seller-3");
        FlakyKafka kafka = new FlakyKafka();
        OutboxRelay relay = new OutboxRelay(mongoTemplate, kafka.template, properties, Clock.systemUTC());

        kafka.failNext = true;
        kafka.failAfter = 1;
        assertThat(relay.relayPending()).as("seul le premier passe, puis Kafka tombe").isEqualTo(1);

        assertThat(reload(first).getPublishedAt()).isNotNull();
        OutboxMessage failed = reload(second);
        assertThat(failed.getPublishedAt()).isNull();
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("Kafka is down");
        assertThat(reload(third).getPublishedAt()).as("jamais publié avant le précédent").isNull();

        kafka.failNext = false;
        assertThat(relay.relayPending()).as("Kafka revenu : reprise dans l'ordre").isEqualTo(2);
        assertThat(kafka.sentSellerIds).containsExactly("seller-1", "seller-2", "seller-2", "seller-3");
        assertThat(reload(third).getPublishedAt()).isNotNull();
    }

    @Test
    void gives_up_on_a_send_that_never_completes_and_keeps_the_order() {
        OutboxMessage stuck = store("seller-1");
        OutboxMessage next = store("seller-2");
        FlakyKafka kafka = new FlakyKafka();
        kafka.hang = true;
        OutboxProperties shortTimeout =
                new OutboxProperties(Duration.ofSeconds(1), 100, Duration.ofSeconds(30), Duration.ofMillis(200), Duration.ofDays(7));
        OutboxRelay relay = new OutboxRelay(mongoTemplate, kafka.template, shortTimeout, Clock.systemUTC());

        assertThat(relay.relayPending()).isZero();

        OutboxMessage failed = reload(stuck);
        assertThat(failed.getPublishedAt()).isNull();
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("TimeoutException");
        assertThat(reload(next).getAttempts()).as("jamais tenté avant le précédent").isZero();
    }

    @Test
    void stops_as_soon_as_another_relay_took_over_the_lock() {
        OutboxMessage first = store("seller-1");
        OutboxMessage second = store("seller-2");
        Instant now = Instant.now();
        OutboxLock lockA = new OutboxLock(mongoTemplate, "instance-a", Duration.ofSeconds(30), Clock.fixed(now, UTC));
        OutboxLock lockB = new OutboxLock(mongoTemplate, "instance-b", Duration.ofSeconds(30),
                Clock.fixed(now.plusSeconds(31), UTC));
        FlakyKafka kafka = new FlakyKafka();
        // Pendant l'envoi du premier message, le verrou de A expire et B le prend
        kafka.duringSend = () -> assertThat(lockB.tryAcquire()).isTrue();
        OutboxRelay relay = new OutboxRelay(mongoTemplate, kafka.template, properties, Clock.systemUTC(), lockA);

        assertThat(relay.relayPending()).as("A s'arrête : B a pris le relais").isEqualTo(1);
        assertThat(kafka.sentSellerIds).containsExactly("seller-1");
        assertThat(reload(first).getPublishedAt()).isNotNull();
        assertThat(reload(second).getAttempts()).as("laissé à B, jamais envoyé par A").isZero();
    }

    @Test
    void only_one_relay_can_hold_the_lock_until_it_is_released_or_expires() throws InterruptedException {
        OutboxLock instanceA = new OutboxLock(mongoTemplate, "instance-a", Duration.ofMillis(500), Clock.systemUTC());
        OutboxLock instanceB = new OutboxLock(mongoTemplate, "instance-b", Duration.ofMillis(500), Clock.systemUTC());

        assertThat(instanceA.tryAcquire()).isTrue();
        assertThat(instanceA.tryAcquire()).as("le détenteur peut prolonger").isTrue();
        assertThat(instanceB.tryAcquire()).as("un autre relais est bloqué").isFalse();

        instanceA.release();
        assertThat(instanceB.tryAcquire()).as("libéré : un autre peut le prendre").isTrue();

        Thread.sleep(700);
        assertThat(instanceA.tryAcquire()).as("expiré (détenteur arrêté brutalement) : repris").isTrue();
    }

    private OutboxMessage store(String sellerId) {
        ProductPublished event = ProductPublished.newBuilder().setProductId("product-1").setSellerId(sellerId).build();
        OutboxMessage message = OutboxMessage.pending("shopflow.product.events", "product-1",
                ProductPublished.class.getName(), AvroJsonCodec.encode(event), metadataFactory.create("product-1", event));
        mongoTemplate.insert(message);
        return message;
    }

    private OutboxMessage reload(OutboxMessage message) {
        return mongoTemplate.findOne(Query.query(where("_id").is(message.getId())), OutboxMessage.class);
    }

    /** Kafka simulé qui peut tomber en panne après un certain nombre d'envois, ou ne jamais répondre. */
    private static final class FlakyKafka {

        @SuppressWarnings("unchecked")
        final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        final List<String> sentSellerIds = new ArrayList<>();
        boolean failNext;
        int failAfter;
        boolean hang;
        Runnable duringSend = () -> { };

        FlakyKafka() {
            when(template.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
                ProducerRecord<String, Object> record = invocation.getArgument(0);
                sentSellerIds.add(((ProductPublished) record.value()).getSellerId());
                duringSend.run();
                if (hang) {
                    return new CompletableFuture<>();
                }
                if (failNext && failAfter-- <= 0) {
                    return CompletableFuture.failedFuture(new IllegalStateException("Kafka is down"));
                }
                return CompletableFuture.completedFuture(mock(SendResult.class));
            });
        }
    }
}
