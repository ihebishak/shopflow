package com.shopflow.testsupport.architecture.fixture.good.infrastructure.mongo;

import com.shopflow.testsupport.architecture.fixture.good.application.spi.ThingRepository;
import com.shopflow.testsupport.architecture.fixture.good.domain.Thing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoThingRepository implements ThingRepository {

    private final MongoTemplate mongoTemplate;

    public MongoThingRepository(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(final Thing thing) {
        mongoTemplate.save(thing);
    }
}
