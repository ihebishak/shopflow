package com.shopflow.starter.outbox;

import com.shopflow.starter.cloudevents.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Message en attente de publication sur Kafka (collection {@code outbox} de chaque service).
 */
@Getter
@AllArgsConstructor
@Document(OutboxMessage.COLLECTION)
final class OutboxMessage {

    static final String COLLECTION = "outbox";

    /** {@code ce_id} du message (UUID v7, strictement croissant : définit l'ordre de publication). */
    @Id
    private final String id;

    private final String topic;

    private final String key;

    /** Nom complet de la classe Avro, pour reconstruire le message. */
    private final String messageType;

    /** Contenu du message en JSON (lisible dans Mongo Express). */
    private final String payload;

    private final EventMetadata metadata;

    private final Instant createdAt;

    /** {@code null} tant que le message n'est pas publié ; supprimé automatiquement après la rétention. */
    private final Instant publishedAt;

    private final int attempts;

    private final String lastError;

    static OutboxMessage pending(String topic, String key, String messageType, String payload, EventMetadata metadata) {
        return new OutboxMessage(metadata.getId(), topic, key, messageType, payload, metadata, metadata.getTime(),
                null, 0, null);
    }
}
