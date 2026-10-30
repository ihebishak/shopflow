package com.shopflow.starter.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages de l'outbox ({@code shopflow.outbox.*}).
 *
 * @param pollInterval intervalle du filet de sécurité (le relais est aussi réveillé à chaque transaction validée)
 * @param batchSize    nombre de messages lus à chaque passage
 * @param lockLease    durée de validité du verrou « un seul relais actif », renouvelé avant chaque message
 * @param sendTimeout  attente maximale de l'accusé de réception Kafka pour un message
 *                     (supérieure à {@code delivery.timeout.ms} : Kafka abandonne avant le relais)
 * @param retention    durée de conservation des messages publiés (index TTL), pour le débogage
 */
@ConfigurationProperties("shopflow.outbox")
public record OutboxProperties(
        @DefaultValue("PT1S") Duration pollInterval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("PT30S") Duration lockLease,
        @DefaultValue("PT15S") Duration sendTimeout,
        @DefaultValue("P7D") Duration retention) {
}
