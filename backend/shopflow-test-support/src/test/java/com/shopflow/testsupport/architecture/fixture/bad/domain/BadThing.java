package com.shopflow.testsupport.architecture.fixture.bad.domain;

import com.shopflow.testsupport.architecture.fixture.bad.application.service.BadThingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.logging.Logger;

/** Viole volontairement les règles du domaine et les règles d'hygiène. */
@Component
public class BadThing {

    private static final Logger LOG = Logger.getLogger("bad");

    private double price;

    @Autowired
    private BadThingService injected;

    public Instant now() {
        return Instant.now();
    }

    public void print() {
        System.out.println(price);
        LOG.info("bad");
    }

    public void fail() {
        throw new RuntimeException("bad");
    }
}
