package com.shopflow.testsupport.architecture.fixture.good.application.api;

import com.shopflow.testsupport.architecture.fixture.good.domain.Thing;

public interface CreateThingUseCase {

    Thing createThing(long priceInCents);
}
