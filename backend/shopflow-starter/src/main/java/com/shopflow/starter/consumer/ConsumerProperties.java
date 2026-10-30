package com.shopflow.starter.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages des consommateurs Kafka ({@code shopflow.consumer.*}).
 *
 * @param retries            nombre de réessais sur place avant la mise en DLT
 * @param initialInterval    attente avant le premier réessai
 * @param multiplier         facteur d'augmentation de l'attente (1 s → 2 s → 4 s → 8 s)
 * @param processedRetention durée de conservation des marqueurs « déjà traité » (au moins la rétention Kafka)
 */
@ConfigurationProperties("shopflow.consumer")
public record ConsumerProperties(
        @DefaultValue("4") int retries,
        @DefaultValue("PT1S") Duration initialInterval,
        @DefaultValue("2") double multiplier,
        @DefaultValue("P7D") Duration processedRetention) {
}
