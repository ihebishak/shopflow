package com.shopflow.testsupport.architecture.fixture.bad.infrastructure.mongo;

import org.springframework.transaction.annotation.Transactional;

/** Viole volontairement la règle « @Transactional uniquement dans application.service ». */
@Transactional
public class BadMongoRepository {
}
