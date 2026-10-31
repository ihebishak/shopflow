package com.shopflow.testsupport.architecture.fixture.good.infrastructure.web.dto.response;

public class ThingResponseDto {

    private final long priceInCents;

    public ThingResponseDto(final long priceInCents) {
        this.priceInCents = priceInCents;
    }

    public long getPriceInCents() {
        return priceInCents;
    }
}
