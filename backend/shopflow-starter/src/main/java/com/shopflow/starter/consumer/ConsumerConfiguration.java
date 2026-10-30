package com.shopflow.starter.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * Politique d'erreur des consommateurs, importée par {@code ShopflowConsumerAutoConfiguration}
 * (dans ce package pour garder les classes internes invisibles des services).
 * Spring Boot branche automatiquement le {@link CommonErrorHandler} sur les conteneurs de @KafkaListener.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumerProperties.class)
public class ConsumerConfiguration {

    @Bean
    ConsumerErrorHandling consumerErrorHandling(ProducerFactory<?, ?> producerFactory,
                                                @Value("${spring.application.name}") String applicationName,
                                                ConsumerProperties properties) {
        return new ConsumerErrorHandling(producerFactory, applicationName, properties);
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    DefaultErrorHandler kafkaErrorHandler(ConsumerErrorHandling errorHandling) {
        return errorHandling.errorHandler();
    }
}
