package com.shopflow.starter.autoconfigure;

import com.shopflow.starter.cloudevents.CloudEventRecordInterceptor;
import com.shopflow.starter.cloudevents.EventMetadataFactory;
import com.shopflow.starter.cloudevents.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;

/**
 * Enveloppe CloudEvents : publication (headers ajoutés) et consommation (corrélation et causalité restaurées).
 * L'intercepteur est branché automatiquement par Spring Boot sur les conteneurs de @KafkaListener.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
public class ShopflowCloudEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock shopflowClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    EventMetadataFactory eventMetadataFactory(@Value("${spring.application.name}") String applicationName,
                                              Clock clock) {
        return new EventMetadataFactory(applicationName, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    CloudEventRecordInterceptor cloudEventRecordInterceptor() {
        return new CloudEventRecordInterceptor();
    }

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    KafkaEventPublisher kafkaEventPublisher(KafkaTemplate<?, ?> kafkaTemplate, EventMetadataFactory factory) {
        return new KafkaEventPublisher(kafkaTemplate, factory);
    }
}
