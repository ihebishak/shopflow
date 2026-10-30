package com.shopflow.starter.consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Marqueur « message déjà traité » (collection {@code processed_messages} de chaque service).
 * Écrit dans la même transaction que le traitement : il existe si et seulement si le traitement a été validé.
 */
@Getter
@AllArgsConstructor
@Document(ProcessedMessage.COLLECTION)
final class ProcessedMessage {

    static final String COLLECTION = "processed_messages";

    /** {@code ce_id} du message : MongoDB interdit deux marqueurs pour le même message. */
    @Id
    private final String id;

    private final String type;

    private final String topic;

    private final int partition;

    private final long offset;

    /** Supprimé automatiquement après la rétention (un doublon ne peut plus arriver). */
    private final Instant processedAt;
}
