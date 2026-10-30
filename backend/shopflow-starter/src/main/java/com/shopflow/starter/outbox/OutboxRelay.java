package com.shopflow.starter.outbox;

import com.shopflow.kernel.id.UuidV7;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Publie sur Kafka les messages de l'outbox, <b>dans l'ordre</b> de leur création.
 * <ul>
 *   <li>Réveillé dès qu'une transaction contenant un message est validée ;</li>
 *   <li>filet de sécurité : passage régulier (crash, Kafka indisponible…) ;</li>
 *   <li>un seul relais actif par service ({@link OutboxLock}) ;</li>
 *   <li>s'arrête au premier échec pour ne jamais publier un message avant celui qui le précède.</li>
 * </ul>
 * Livraison « au moins une fois » : un crash entre l'envoi et le marquage provoque un renvoi,
 * absorbé par les consommateurs idempotents.
 */
public class OutboxRelay implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxProperties properties;
    private final OutboxLock lock;
    private final Clock clock;
    private final AtomicBoolean wakeUpPending = new AtomicBoolean();

    private volatile ScheduledExecutorService scheduler;

    public OutboxRelay(MongoTemplate mongoTemplate, KafkaTemplate<?, ?> kafkaTemplate,
                       OutboxProperties properties, Clock clock) {
        this(mongoTemplate, kafkaTemplate, properties, clock,
                new OutboxLock(mongoTemplate, UuidV7.generate().toString(), properties.getLockLease(), clock));
    }

    @SuppressWarnings("unchecked")
    OutboxRelay(MongoTemplate mongoTemplate, KafkaTemplate<?, ?> kafkaTemplate,
                OutboxProperties properties, Clock clock, OutboxLock lock) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = (KafkaTemplate<String, Object>) kafkaTemplate;
        this.properties = properties;
        this.clock = clock;
        this.lock = lock;
    }

    /** Demande un passage immédiat (plusieurs demandes rapprochées n'en déclenchent qu'un). */
    public void wakeUp() {
        ScheduledExecutorService current = scheduler;
        if (current != null && wakeUpPending.compareAndSet(false, true)) {
            try {
                current.execute(() -> {
                    wakeUpPending.set(false);
                    relaySafely();
                });
            } catch (RejectedExecutionException shuttingDown) {
                wakeUpPending.set(false);
            }
        }
    }

    /**
     * Publie les messages en attente, dans l'ordre, jusqu'à épuisement ou premier échec.
     *
     * @return le nombre de messages publiés
     */
    int relayPending() {
        int published = 0;
        try {
            while (lock.tryAcquire()) {
                List<OutboxMessage> batch = mongoTemplate.find(Query.query(where("publishedAt").is(null))
                        .with(Sort.by("_id")).limit(properties.getBatchSize()), OutboxMessage.class);
                if (batch.isEmpty()) {
                    return published;
                }
                for (OutboxMessage message : batch) {
                    // Renouvelé avant chaque envoi : le verrou couvre toujours l'envoi en cours en entier
                    if (!lock.tryAcquire() || !publish(message)) {
                        return published;
                    }
                    published++;
                }
            }
            return published;
        } finally {
            lock.release();
        }
    }

    private boolean publish(OutboxMessage message) {
        try {
            SpecificRecord payload = AvroJsonCodec.decode(message.getPayload(), message.getMessageType());
            kafkaTemplate.send(new ProducerRecord<>(message.getTopic(), null, message.getKey(), payload,
                            message.getMetadata().toHeaders()))
                    .get(properties.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            mongoTemplate.updateFirst(Query.query(where("_id").is(message.getId())),
                    new Update().set("publishedAt", clock.instant()).inc("attempts", 1).unset("lastError"),
                    OutboxMessage.class);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(message, e);
            return false;
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            recordFailure(message, e);
            return false;
        }
    }

    private void recordFailure(OutboxMessage message, Exception error) {
        Throwable cause = error instanceof ExecutionException && error.getCause() != null ? error.getCause() : error;
        String description = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        log.warn("Outbox: publication of {} ({}) failed, will retry: {}", message.getId(), message.getMetadata().getType(),
                description);
        mongoTemplate.updateFirst(Query.query(where("_id").is(message.getId())),
                new Update().inc("attempts", 1).set("lastError",
                        description.length() > MAX_ERROR_LENGTH ? description.substring(0, MAX_ERROR_LENGTH) : description),
                OutboxMessage.class);
    }

    private void relaySafely() {
        try {
            relayPending();
        } catch (RuntimeException e) {
            log.warn("Outbox relay run failed, will retry", e);
        }
    }

    @Override
    public void start() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "outbox-relay");
            thread.setDaemon(true);
            return thread;
        });
        long interval = properties.getPollInterval().toMillis();
        executor.scheduleWithFixedDelay(this::relaySafely, interval, interval, TimeUnit.MILLISECONDS);
        scheduler = executor;
    }

    @Override
    public void stop() {
        ScheduledExecutorService executor = scheduler;
        scheduler = null;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(properties.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return scheduler != null;
    }
}
