package com.shopflow.starter.cloudevents;

import com.shopflow.starter.correlation.Correlation;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Avant chaque message consommé : lit l'enveloppe CloudEvents, place la corrélation dans le MDC
 * et mémorise le message en cours (causalité). Nettoie le thread après le traitement.
 */
public class CloudEventRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer) {
        EventMetadata.fromHeaders(record.headers()).ifPresentOrElse(
                metadata -> {
                    MessageContext.set(metadata);
                    Correlation.set(metadata.getCorrelationId());
                },
                () -> Correlation.set(Correlation.newId()));
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        MessageContext.clear();
        Correlation.clear();
    }
}
