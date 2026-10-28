package com.shopflow.starter.cloudevents;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.shopflow.starter.cloudevents.CloudEventHeaders.*;

/**
 * Métadonnées CloudEvents d'un message Kafka. Le contenu métier (Avro) reste dans la valeur du message.
 *
 * @param causationId {@code ce_id} du message qui a provoqué celui-ci, {@code null} pour un message initial
 */
public record EventMetadata(String id, String type, String source, String subject, Instant time,
                            String correlationId, String causationId) {

    public EventMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(correlationId, "correlationId");
    }

    public Headers toHeaders() {
        RecordHeaders headers = new RecordHeaders();
        add(headers, SPEC_VERSION, SPEC_VERSION_VALUE);
        add(headers, ID, id);
        add(headers, TYPE, type);
        add(headers, SOURCE, source);
        add(headers, TIME, time.toString());
        add(headers, SUBJECT, subject);
        add(headers, CORRELATION_ID, correlationId);
        add(headers, CAUSATION_ID, causationId);
        add(headers, CONTENT_TYPE, AVRO_CONTENT_TYPE);
        return headers;
    }

    /** Lit l'enveloppe d'un message reçu ; vide si le message n'est pas un CloudEvent. */
    public static Optional<EventMetadata> fromHeaders(Headers headers) {
        String id = read(headers, ID);
        String type = read(headers, TYPE);
        String source = read(headers, SOURCE);
        String time = read(headers, TIME);
        String correlationId = read(headers, CORRELATION_ID);
        if (id == null || type == null || source == null || time == null || correlationId == null) {
            return Optional.empty();
        }
        return Optional.of(new EventMetadata(id, type, source, read(headers, SUBJECT), Instant.parse(time),
                correlationId, read(headers, CAUSATION_ID)));
    }

    private static void add(Headers headers, String key, String value) {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
