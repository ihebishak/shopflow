package com.shopflow.starter.outbox;

import com.shopflow.starter.cloudevents.EventMetadata;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Message en attente de publication sur Kafka (collection {@code outbox} de chaque service).
 *
 * @param id          {@code ce_id} du message (UUID v7, strictement croissant : définit l'ordre de publication)
 * @param messageType nom complet de la classe Avro, pour reconstruire le message
 * @param payload     contenu du message en JSON (lisible dans Mongo Express)
 * @param publishedAt {@code null} tant que le message n'est pas publié ; supprimé automatiquement après la rétention
 */
@Document(OutboxMessage.COLLECTION)
record OutboxMessage(
        @Id String id,
        String topic,
        String key,
        String messageType,
        String payload,
        EventMetadata metadata,
        Instant createdAt,
        Instant publishedAt,
        int attempts,
        String lastError) {

    static final String COLLECTION = "outbox";

    static OutboxMessage pending(String topic, String key, String messageType, String payload, EventMetadata metadata) {
        return new OutboxMessage(metadata.id(), topic, key, messageType, payload, metadata, metadata.time(),
                null, 0, null);
    }
}
