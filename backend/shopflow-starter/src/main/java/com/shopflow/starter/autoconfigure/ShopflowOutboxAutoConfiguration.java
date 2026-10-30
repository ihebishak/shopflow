package com.shopflow.starter.autoconfigure;

import com.shopflow.starter.outbox.OutboxConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.TransactionManager;

/**
 * Services MongoDB : transactions (@Transactional) et outbox.
 * <p>
 * Déclarée avant {@link TransactionAutoConfiguration} : Spring Boot n'active la gestion de @Transactional
 * que s'il trouve un gestionnaire de transactions à ce moment-là.
 */
@AutoConfiguration(
        after = {MongoDataAutoConfiguration.class, KafkaAutoConfiguration.class, ShopflowCloudEventsAutoConfiguration.class},
        before = TransactionAutoConfiguration.class)
@ConditionalOnClass(MongoTemplate.class)
public class ShopflowOutboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(MongoDatabaseFactory.class)
    @ConditionalOnMissingBean(TransactionManager.class)
    MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean({MongoTemplate.class, KafkaTemplate.class})
    @Import(OutboxConfiguration.class)
    static class Outbox {
    }
}
