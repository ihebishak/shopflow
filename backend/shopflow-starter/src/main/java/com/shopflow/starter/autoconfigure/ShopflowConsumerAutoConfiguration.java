package com.shopflow.starter.autoconfigure;

import com.shopflow.starter.consumer.ConsumerConfiguration;
import com.shopflow.starter.consumer.IdempotencyConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Consommateurs Kafka : réessais sur place puis DLT (tous les services),
 * et idempotence dans une transaction MongoDB (services MongoDB).
 */
@AutoConfiguration(after = {KafkaAutoConfiguration.class, ShopflowCloudEventsAutoConfiguration.class,
        ShopflowOutboxAutoConfiguration.class})
@ConditionalOnBean(ProducerFactory.class)
@Import(ConsumerConfiguration.class)
public class ShopflowConsumerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MongoTemplate.class)
    @ConditionalOnBean({MongoTemplate.class, MongoTransactionManager.class})
    @Import(IdempotencyConfiguration.class)
    static class Idempotency {
    }
}
