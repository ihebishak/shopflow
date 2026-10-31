package com.shopflow.testsupport.architecture.fixture.good.application.service;

import com.shopflow.testsupport.architecture.fixture.good.application.api.CreateThingUseCase;
import com.shopflow.testsupport.architecture.fixture.good.application.spi.ThingRepository;
import com.shopflow.testsupport.architecture.fixture.good.domain.Thing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CreateThingService implements CreateThingUseCase {

    private final ThingRepository thingRepository;
    private final Clock clock;

    public CreateThingService(final ThingRepository thingRepository, final Clock clock) {
        this.thingRepository = thingRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Thing createThing(final long priceInCents) {
        Thing thing = new Thing(priceInCents, clock.instant());
        thingRepository.save(thing);
        return thing;
    }
}
