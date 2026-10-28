package com.shopflow.starter.cloudevents;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Publie un événement ou une commande Avro sur Kafka avec son enveloppe CloudEvents.
 * La clé du message est l'identifiant de l'entité (docs/03-conventions.md § 1.1).
 */
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventMetadataFactory metadataFactory;

    @SuppressWarnings("unchecked")
    public KafkaEventPublisher(KafkaTemplate<?, ?> kafkaTemplate, EventMetadataFactory metadataFactory) {
        this.kafkaTemplate = (KafkaTemplate<String, Object>) kafkaTemplate;
        this.metadataFactory = metadataFactory;
    }

    public CompletableFuture<SendResult<String, Object>> publish(String topic, String key, SpecificRecord message) {
        EventMetadata metadata = metadataFactory.create(key, message);
        return kafkaTemplate.send(new ProducerRecord<>(topic, null, key, message, metadata.toHeaders()));
    }
}
