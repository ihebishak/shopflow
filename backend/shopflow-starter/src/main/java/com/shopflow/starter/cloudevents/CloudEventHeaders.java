package com.shopflow.starter.cloudevents;

/**
 * Noms des headers Kafka de l'enveloppe CloudEvents (binding Kafka, mode binaire).
 * Voir docs/03-conventions.md § 1.3.
 */
public final class CloudEventHeaders {

    public static final String SPEC_VERSION = "ce_specversion";
    public static final String ID = "ce_id";
    public static final String TYPE = "ce_type";
    public static final String SOURCE = "ce_source";
    public static final String TIME = "ce_time";
    public static final String SUBJECT = "ce_subject";
    public static final String CORRELATION_ID = "ce_correlationid";
    public static final String CAUSATION_ID = "ce_causationid";
    public static final String CONTENT_TYPE = "content-type";

    public static final String SPEC_VERSION_VALUE = "1.0";
    public static final String AVRO_CONTENT_TYPE = "application/avro";

    private CloudEventHeaders() {
    }
}
