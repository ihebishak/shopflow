package com.shopflow.starter.consumer;

import com.shopflow.starter.cloudevents.EventMetadata;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Clock;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Enveloppe chaque {@code @KafkaListener} dans une transaction MongoDB et ne le laisse traiter un message qu'une fois.
 * <p>
 * Traitement, marqueur {@link ProcessedMessage} et messages publiés via l'outbox : tout ou rien.
 * En cas d'échec, tout est annulé (marqueur compris) et le message est réessayé normalement.
 * Le marqueur est vérifié au début mais inséré à la fin : dans une transaction MongoDB,
 * une insertion refusée annulerait toute la transaction.
 */
public class IdempotentListenerAdvice implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentListenerAdvice.class);

    private final MongoTemplate mongoTemplate;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public IdempotentListenerAdvice(MongoTemplate mongoTemplate, PlatformTransactionManager transactionManager,
                                    Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    public Object invoke(MethodInvocation invocation) {
        ConsumerRecord<?, ?> record = recordOf(invocation);
        if (record == null) {
            return proceed(invocation);
        }
        Optional<EventMetadata> metadata = EventMetadata.fromHeaders(record.headers());
        if (metadata.isEmpty()) {
            log.warn("Message without ce_id from {}-{}@{}: processed without duplicate protection",
                    record.topic(), record.partition(), record.offset());
        }
        return transaction.execute(status -> {
            if (metadata.isPresent() && alreadyProcessed(metadata.get())) {
                log.info("Duplicate message {} ({}) ignored", metadata.get().getId(), metadata.get().getType());
                return null;
            }
            Object result = proceed(invocation);
            metadata.ifPresent(m -> mongoTemplate.insert(new ProcessedMessage(m.getId(), m.getType(), record.topic(),
                    record.partition(), record.offset(), clock.instant())));
            return result;
        });
    }

    private boolean alreadyProcessed(EventMetadata metadata) {
        return mongoTemplate.exists(query(where("_id").is(metadata.getId())), ProcessedMessage.class);
    }

    /** Le message reçu, si l'appel est la réception d'un message (et non une autre méthode du listener). */
    private static ConsumerRecord<?, ?> recordOf(MethodInvocation invocation) {
        if (!"onMessage".equals(invocation.getMethod().getName())) {
            return null;
        }
        for (Object argument : invocation.getArguments()) {
            if (argument instanceof ConsumerRecord<?, ?> record) {
                return record;
            }
        }
        return null;
    }

    private static Object proceed(MethodInvocation invocation) {
        try {
            return invocation.proceed();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
