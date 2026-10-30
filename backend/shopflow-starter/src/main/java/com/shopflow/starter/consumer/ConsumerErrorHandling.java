package com.shopflow.starter.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Politique d'erreur des consommateurs : réessais <b>sur place</b> (l'ordre des messages est préservé),
 * puis mise de côté dans la DLT du service, {@code <topic>.<service>.dlt}, pour passer au message suivant.
 * Les erreurs définitives (message illisible, mauvais type…) partent directement en DLT.
 */
class ConsumerErrorHandling implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ConsumerErrorHandling.class);

    private final String applicationName;
    private final ConsumerProperties properties;
    private final DefaultKafkaProducerFactory<Object, Object> avroProducerFactory;
    private final DefaultKafkaProducerFactory<Object, Object> rawProducerFactory;

    ConsumerErrorHandling(ProducerFactory<?, ?> producerFactory, String applicationName, ConsumerProperties properties) {
        this.applicationName = applicationName;
        this.properties = properties;
        Map<String, Object> avro = new HashMap<>(producerFactory.getConfigurationProperties());
        // Le schéma est une copie de celui, déjà validé par la CI, du topic d'origine (docs/03-conventions.md)
        avro.put("auto.register.schemas", "true");
        this.avroProducerFactory = new DefaultKafkaProducerFactory<>(avro);
        Map<String, Object> raw = new HashMap<>(producerFactory.getConfigurationProperties());
        raw.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        this.rawProducerFactory = new DefaultKafkaProducerFactory<>(raw);
    }

    static String deadLetterTopic(String topic, String applicationName) {
        return topic + "." + applicationName + ".dlt";
    }

    DefaultErrorHandler errorHandler() {
        // Message illisible : ses octets bruts ; sinon le message Avro d'origine
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, new KafkaTemplate<>(rawProducerFactory));
        templates.put(Object.class, new KafkaTemplate<>(avroProducerFactory));
        // Partition -1 : Kafka la choisit d'après la clé, les messages d'une même entité restent dans l'ordre
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, exception) -> new TopicPartition(deadLetterTopic(record.topic(), applicationName), -1));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(properties.retries());
        backOff.setInitialInterval(properties.initialInterval().toMillis());
        backOff.setMultiplier(properties.multiplier());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
                log.warn("Processing of {}-{}@{} failed (attempt {}): {}", record.topic(), record.partition(),
                        record.offset(), deliveryAttempt, rootCause(exception));
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
                log.error("Message {}-{}@{} moved to {}: {}", record.topic(), record.partition(), record.offset(),
                        deadLetterTopic(record.topic(), applicationName), rootCause(exception));
            }
        });
        return errorHandler;
    }

    private static String rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    @Override
    public void destroy() {
        avroProducerFactory.destroy();
        rawProducerFactory.destroy();
    }
}
