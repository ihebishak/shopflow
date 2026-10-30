package com.shopflow.starter.cloudevents;

import com.shopflow.contracts.product.ProductPublished;
import com.shopflow.starter.correlation.Correlation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetadataFactoryTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    private final EventMetadataFactory factory =
            new EventMetadataFactory("catalog-service", Clock.fixed(NOW, ZoneOffset.UTC));

    private final ProductPublished event =
            ProductPublished.newBuilder().setProductId("product-1").setSellerId("seller-1").build();

    @AfterEach
    void clearThread() {
        Correlation.clear();
        MessageContext.clear();
    }

    @Test
    void fills_the_envelope_from_the_message_and_the_application() {
        EventMetadata metadata = factory.create("product-1", event);

        assertThat(UUID.fromString(metadata.getId()).version()).isEqualTo(7);
        assertThat(metadata.getType()).isEqualTo("shopflow.product.ProductPublished.v1");
        assertThat(metadata.getSource()).isEqualTo("/catalog-service");
        assertThat(metadata.getSubject()).isEqualTo("product-1");
        assertThat(metadata.getTime()).isEqualTo(NOW);
    }

    @Test
    void reuses_the_correlation_id_of_the_current_request() {
        Correlation.set("corr-from-http");

        assertThat(factory.create("product-1", event).getCorrelationId()).isEqualTo("corr-from-http");
    }

    @Test
    void chains_correlation_and_causation_from_the_message_being_processed() {
        EventMetadata incoming = new EventMetadata("incoming-id", "shopflow.product.ProductCreated.v1",
                "/catalog-service", "product-1", NOW, "corr-from-kafka", null);
        MessageContext.set(incoming);

        EventMetadata metadata = factory.create("product-1", event);

        assertThat(metadata.getCorrelationId()).isEqualTo("corr-from-kafka");
        assertThat(metadata.getCausationId()).isEqualTo("incoming-id");
    }

    @Test
    void starts_a_new_journey_when_there_is_no_context() {
        EventMetadata metadata = factory.create("product-1", event);

        assertThat(metadata.getCorrelationId()).isNotBlank();
        assertThat(metadata.getCausationId()).isNull();
    }
}
