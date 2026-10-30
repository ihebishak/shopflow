package com.shopflow.catalog;

import com.shopflow.starter.consumer.IdempotentListenerAdvice;
import com.shopflow.starter.outbox.OutboxPublisher;
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
    ApplicationContext context;

    @Autowired
    TestRestTemplate http;

    @Test
    @SuppressWarnings("unchecked")
    void starts_and_reports_itself_healthy() {
        Map<String, Object> health = http.getForObject("/actuator/health", Map.class);

        assertThat(health).containsEntry("status", "UP");
        assertThat((Map<String, Object>) health.get("components")).containsKey("mongo");
    }

    @Test
    void gets_the_starter_foundations_without_any_configuration() {
        assertThat(context.getBean(OutboxPublisher.class)).as("outbox").isNotNull();
        assertThat(context.getBean(IdempotentListenerAdvice.class)).as("consommateurs idempotents").isNotNull();
        assertThat(context.getBean(CommonErrorHandler.class)).as("réessais puis DLT")
                .isInstanceOf(DefaultErrorHandler.class);
    }
}
