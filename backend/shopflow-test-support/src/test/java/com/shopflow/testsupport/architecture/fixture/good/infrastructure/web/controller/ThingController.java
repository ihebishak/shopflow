package com.shopflow.testsupport.architecture.fixture.good.infrastructure.web.controller;

import com.shopflow.testsupport.architecture.fixture.good.application.api.CreateThingUseCase;
import com.shopflow.testsupport.architecture.fixture.good.infrastructure.web.dto.response.ThingResponseDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThingController {

    private final CreateThingUseCase createThingUseCase;

    public ThingController(final CreateThingUseCase createThingUseCase) {
        this.createThingUseCase = createThingUseCase;
    }

    @PostMapping("/things")
    public ThingResponseDto createThing() {
        return new ThingResponseDto(createThingUseCase.createThing(100).getPriceInCents());
    }
}
