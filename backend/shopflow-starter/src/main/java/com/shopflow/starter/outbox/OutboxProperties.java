package com.shopflow.starter.outbox;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages de l'outbox ({@code shopflow.outbox.*}).
 * Constructeur écrit à la main : les valeurs par défaut se déclarent sur ses paramètres.
 */
@Getter
@ConfigurationProperties("shopflow.outbox")
public class OutboxProperties {

    /** Intervalle du filet de sécurité (le relais est aussi réveillé à chaque transaction validée). */
    private final Duration pollInterval;

    /** Nombre de messages lus à chaque passage. */
    private final int batchSize;

    /** Durée de validité du verrou « un seul relais actif », renouvelé avant chaque message. */
    private final Duration lockLease;

    /**
     * Attente maximale de l'accusé de réception Kafka pour un message
     * (supérieure à {@code delivery.timeout.ms} : Kafka abandonne avant le relais).
     */
    private final Duration sendTimeout;

    /** Durée de conservation des messages publiés (index TTL), pour le débogage. */
    private final Duration retention;

    public OutboxProperties(@DefaultValue("PT1S") Duration pollInterval,
                            @DefaultValue("100") int batchSize,
                            @DefaultValue("PT30S") Duration lockLease,
                            @DefaultValue("PT15S") Duration sendTimeout,
                            @DefaultValue("P7D") Duration retention) {
        this.pollInterval = pollInterval;
        this.batchSize = batchSize;
        this.lockLease = lockLease;
        this.sendTimeout = sendTimeout;
        this.retention = retention;
    }
}
