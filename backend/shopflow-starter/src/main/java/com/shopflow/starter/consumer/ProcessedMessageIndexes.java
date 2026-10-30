package com.shopflow.starter.consumer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Crée au démarrage l'index de suppression automatique des marqueurs, une fois la rétention écoulée.
 */
class ProcessedMessageIndexes implements InitializingBean {

    private final MongoTemplate mongoTemplate;
    private final ConsumerProperties properties;

    ProcessedMessageIndexes(MongoTemplate mongoTemplate, ConsumerProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        mongoTemplate.indexOps(ProcessedMessage.COLLECTION).ensureIndex(new Index()
                .on("processedAt", Sort.Direction.ASC).expire(properties.getProcessedRetention()).named("processed_ttl"));
    }
}
