package com.shopflow.starter.outbox;

import com.shopflow.starter.cloudevents.EventMetadataFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Beans de l'outbox, importés par {@code ShopflowOutboxAutoConfiguration}
 * (dans ce package pour garder les classes internes de l'outbox invisibles des services).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OutboxRelay outboxRelay(MongoTemplate mongoTemplate, KafkaTemplate<?, ?> kafkaTemplate,
                            OutboxProperties properties, Clock clock) {
        checkTimeouts(properties, kafkaTemplate.getProducerFactory().getConfigurationProperties());
        return new OutboxRelay(mongoTemplate, kafkaTemplate, properties, clock);
    }

    /**
     * Un envoi bloqué ne doit jamais survivre au verrou, sinon un second relais publierait en parallèle (ordre cassé) :
     * Kafka abandonne avant le relais, et l'envoi complet (blocage + attente de l'accusé) tient dans le verrou.
     */
    static void checkTimeouts(OutboxProperties properties, Map<String, Object> producerConfig) {
        Duration maxBlock = kafkaDuration(producerConfig, ProducerConfig.MAX_BLOCK_MS_CONFIG, 60_000);
        Duration deliveryTimeout = kafkaDuration(producerConfig, ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        if (properties.getSendTimeout().compareTo(deliveryTimeout) <= 0) {
            throw new IllegalStateException("shopflow.outbox.send-timeout (" + properties.getSendTimeout()
                    + ") must exceed the Kafka producer delivery.timeout.ms (" + deliveryTimeout
                    + "): a message the relay gave up on could still be delivered later");
        }
        Duration longestSend = maxBlock.plus(properties.getSendTimeout());
        if (properties.getLockLease().compareTo(longestSend) <= 0) {
            throw new IllegalStateException("shopflow.outbox.lock-lease (" + properties.getLockLease()
                    + ") must exceed max.block.ms + send-timeout (" + longestSend
                    + "): a stuck relay could lose its lock while still publishing");
        }
    }

    private static Duration kafkaDuration(Map<String, Object> producerConfig, String key, long kafkaDefaultMillis) {
        Object value = producerConfig.get(key);
        return Duration.ofMillis(value == null ? kafkaDefaultMillis : Long.parseLong(value.toString().trim()));
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxPublisher outboxPublisher(MongoTemplate mongoTemplate, EventMetadataFactory metadataFactory,
                                    OutboxRelay relay) {
        return new OutboxPublisher(mongoTemplate, metadataFactory, relay);
    }

    @Bean
    OutboxIndexes outboxIndexes(MongoTemplate mongoTemplate, OutboxProperties properties) {
        return new OutboxIndexes(mongoTemplate, properties);
    }
}
