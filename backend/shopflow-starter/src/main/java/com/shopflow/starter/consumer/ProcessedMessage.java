package com.shopflow.starter.consumer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Marqueur « message déjà traité » (collection {@code processed_messages} de chaque service).
 * Écrit dans la même transaction que le traitement : il existe si et seulement si le traitement a été validé.
 *
 * @param id          {@code ce_id} du message : MongoDB interdit deux marqueurs pour le même message
 * @param processedAt supprimé automatiquement après la rétention (un doublon ne peut plus arriver)
 */
@Document(ProcessedMessage.COLLECTION)
record ProcessedMessage(
        @Id String id,
        String type,
        String topic,
        int partition,
        long offset,
        Instant processedAt) {

    static final String COLLECTION = "processed_messages";
}
