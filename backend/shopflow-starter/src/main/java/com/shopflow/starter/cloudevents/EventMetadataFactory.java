package com.shopflow.starter.cloudevents;

import com.shopflow.kernel.id.UuidV7;
import com.shopflow.starter.correlation.Correlation;
import org.apache.avro.specific.SpecificRecord;

import java.time.Clock;
import java.util.Optional;

/**
 * Construit l'enveloppe d'un message à publier à partir du contexte courant :
 * corrélation (MDC, sinon message en cours, sinon nouvelle) et causalité (message en cours).
 */
public class EventMetadataFactory {

    private final String source;
    private final Clock clock;

    public EventMetadataFactory(String applicationName, Clock clock) {
        this.source = "/" + applicationName;
        this.clock = clock;
    }

    public EventMetadata create(String subject, SpecificRecord message) {
        Optional<EventMetadata> cause = MessageContext.current();
        String correlationId = Correlation.current()
                .or(() -> cause.map(EventMetadata::correlationId))
                .orElseGet(Correlation::newId);
        return new EventMetadata(
                UuidV7.generate().toString(),
                CloudEventTypes.typeOf(message.getSchema()),
                source,
                subject,
                clock.instant(),
                correlationId,
                cause.map(EventMetadata::id).orElse(null));
    }
}
