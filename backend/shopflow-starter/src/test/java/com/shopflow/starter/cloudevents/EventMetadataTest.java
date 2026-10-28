package com.shopflow.starter.cloudevents;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetadataTest {

    private final EventMetadata metadata = new EventMetadata(
            "0192f0c4-0000-7000-8000-000000000001",
            "shopflow.product.ProductCreated.v1",
            "/catalog-service",
            "product-1",
            Instant.parse("2026-10-28T10:00:00Z"),
            "corr-1",
            "0192f0c4-0000-7000-8000-000000000000");

    @Test
    void survives_a_round_trip_through_kafka_headers() {
        assertThat(EventMetadata.fromHeaders(metadata.toHeaders())).contains(metadata);
    }

    @Test
    void writes_cloudevents_spec_version_and_avro_content_type() {
        Headers headers = metadata.toHeaders();

        assertThat(value(headers, CloudEventHeaders.SPEC_VERSION)).isEqualTo("1.0");
        assertThat(value(headers, CloudEventHeaders.CONTENT_TYPE)).isEqualTo("application/avro");
        assertThat(value(headers, CloudEventHeaders.TIME)).isEqualTo("2026-10-28T10:00:00Z");
    }

    @Test
    void omits_causation_header_for_an_initial_message() {
        EventMetadata initial = new EventMetadata(metadata.id(), metadata.type(), metadata.source(),
                metadata.subject(), metadata.time(), metadata.correlationId(), null);

        assertThat(initial.toHeaders().lastHeader(CloudEventHeaders.CAUSATION_ID)).isNull();
        assertThat(EventMetadata.fromHeaders(initial.toHeaders())).contains(initial);
    }

    @Test
    void ignores_messages_that_are_not_cloudevents() {
        assertThat(EventMetadata.fromHeaders(new RecordHeaders())).isEmpty();
    }

    private static String value(Headers headers, String key) {
        return new String(headers.lastHeader(key).value(), StandardCharsets.UTF_8);
    }
}
