package com.shopflow.starter.outbox;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * Crée au démarrage les index de l'outbox :
 * lecture rapide des messages en attente dans l'ordre, et suppression automatique des messages publiés.
 */
class OutboxIndexes implements InitializingBean {

    private final MongoTemplate mongoTemplate;
    private final OutboxProperties properties;

    OutboxIndexes(MongoTemplate mongoTemplate, OutboxProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        IndexOperations indexes = mongoTemplate.indexOps(OutboxMessage.COLLECTION);
        indexes.ensureIndex(new Index().on("publishedAt", Sort.Direction.ASC).on("_id", Sort.Direction.ASC)
                .named("pending_in_order"));
        // Un index TTL ignore les documents sans date : les messages non publiés ne sont jamais supprimés
        indexes.ensureIndex(new Index().on("publishedAt", Sort.Direction.ASC)
                .expire(properties.getRetention()).named("published_ttl"));
    }
}
