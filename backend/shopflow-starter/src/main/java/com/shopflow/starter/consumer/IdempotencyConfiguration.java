package com.shopflow.starter.consumer;

import org.aopalliance.aop.Advice;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Clock;
import java.util.Arrays;

/**
 * Consommateurs idempotents (services MongoDB), importés par {@code ShopflowConsumerAutoConfiguration}.
 * Spring Boot applique le {@link ContainerCustomizer} à chaque conteneur de @KafkaListener.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumerProperties.class)
public class IdempotencyConfiguration {

    @Bean
    IdempotentListenerAdvice idempotentListenerAdvice(MongoTemplate mongoTemplate,
                                                      MongoTransactionManager transactionManager, Clock clock) {
        return new IdempotentListenerAdvice(mongoTemplate, transactionManager, clock);
    }

    @Bean
    ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> idempotentListeners(
            IdempotentListenerAdvice advice) {
        return container -> {
            ContainerProperties properties = container.getContainerProperties();
            Advice[] chain = Arrays.copyOf(properties.getAdviceChain(), properties.getAdviceChain().length + 1);
            chain[chain.length - 1] = advice;
            properties.setAdviceChain(chain);
        };
    }

    @Bean
    ProcessedMessageIndexes processedMessageIndexes(MongoTemplate mongoTemplate, ConsumerProperties properties) {
        return new ProcessedMessageIndexes(mongoTemplate, properties);
    }
}
