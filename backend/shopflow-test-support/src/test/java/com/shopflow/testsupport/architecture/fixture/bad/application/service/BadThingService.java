package com.shopflow.testsupport.architecture.fixture.bad.application.service;

import com.shopflow.testsupport.architecture.fixture.bad.domain.BadThing;
import com.shopflow.testsupport.architecture.fixture.bad.infrastructure.mongo.BadMongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

/** Viole volontairement les règles de la couche application. */
public class BadThingService {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BadMongoRepository badMongoRepository;
    private final BadThing badThing;

    public BadThingService(final MongoTemplate mongoTemplate, final KafkaTemplate<String, String> kafkaTemplate,
                           final BadMongoRepository badMongoRepository, final BadThing badThing) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.badMongoRepository = badMongoRepository;
        this.badThing = badThing;
    }
}
