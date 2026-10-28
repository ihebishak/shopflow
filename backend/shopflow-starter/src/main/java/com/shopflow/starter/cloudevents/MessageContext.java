package com.shopflow.starter.cloudevents;

import java.util.Optional;

/**
 * Message Kafka en cours de traitement sur le thread courant. Permet de renseigner automatiquement
 * le {@code ce_causationid} des messages publiés pendant ce traitement.
 */
public final class MessageContext {

    private static final ThreadLocal<EventMetadata> CURRENT = new ThreadLocal<>();

    private MessageContext() {
    }

    public static Optional<EventMetadata> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    static void set(EventMetadata metadata) {
        CURRENT.set(metadata);
    }

    static void clear() {
        CURRENT.remove();
    }
}
