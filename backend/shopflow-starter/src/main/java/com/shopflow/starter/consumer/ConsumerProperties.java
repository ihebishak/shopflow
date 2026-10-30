package com.shopflow.starter.consumer;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages des consommateurs Kafka ({@code shopflow.consumer.*}).
 * Constructeur écrit à la main : les valeurs par défaut se déclarent sur ses paramètres.
 */
@Getter
@ConfigurationProperties("shopflow.consumer")
public class ConsumerProperties {

    /** Nombre de réessais sur place avant la mise en DLT. */
    private final int retries;

    /** Attente avant le premier réessai. */
    private final Duration initialInterval;

    /** Facteur d'augmentation de l'attente (1 s → 2 s → 4 s → 8 s). */
    private final double multiplier;

    /** Durée de conservation des marqueurs « déjà traité » (au moins la rétention Kafka). */
    private final Duration processedRetention;

    public ConsumerProperties(@DefaultValue("4") int retries,
                              @DefaultValue("PT1S") Duration initialInterval,
                              @DefaultValue("2") double multiplier,
                              @DefaultValue("P7D") Duration processedRetention) {
        this.retries = retries;
        this.initialInterval = initialInterval;
        this.multiplier = multiplier;
        this.processedRetention = processedRetention;
    }
}
