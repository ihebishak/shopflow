package com.shopflow.testsupport.architecture.fixture.bad.infrastructure.web.controller;

import com.shopflow.testsupport.architecture.fixture.bad.application.service.BadThingService;
import com.shopflow.testsupport.architecture.fixture.bad.domain.BadThing;
import com.shopflow.testsupport.architecture.fixture.bad.infrastructure.mongo.BadMongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/** Viole volontairement les règles de la couche web (son nom ne finit pas par Controller). */
public class ThingEndpoint {

    private final BadThingService badThingService;
    private final BadMongoRepository badMongoRepository;

    public ThingEndpoint(final BadThingService badThingService, final BadMongoRepository badMongoRepository) {
        this.badThingService = badThingService;
        this.badMongoRepository = badMongoRepository;
    }

    @Transactional
    public BadThing direct() {
        return new BadThing();
    }

    public ResponseEntity<BadThing> wrapped() {
        return ResponseEntity.ok(new BadThing());
    }
}
