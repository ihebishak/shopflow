package com.shopflow.testsupport.architecture.fixture.good.application.spi;

import com.shopflow.testsupport.architecture.fixture.good.domain.Thing;

public interface ThingRepository {

    void save(Thing thing);
}
