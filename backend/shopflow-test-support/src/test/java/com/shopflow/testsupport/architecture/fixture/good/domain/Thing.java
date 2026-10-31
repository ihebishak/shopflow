package com.shopflow.testsupport.architecture.fixture.good.domain;

import java.time.Instant;

public final class Thing {

    private final long priceInCents;
    private final Instant createdAt;

    public Thing(final long priceInCents, final Instant createdAt) {
        this.priceInCents = priceInCents;
        this.createdAt = createdAt;
    }

    public long getPriceInCents() {
        return priceInCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
