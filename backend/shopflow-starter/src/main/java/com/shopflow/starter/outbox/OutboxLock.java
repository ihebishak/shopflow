package com.shopflow.starter.outbox;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Verrou « un seul relais actif » par service, même avec plusieurs instances : garantit l'ordre de publication.
 * Un seul document ; le verrou expire de lui-même si son détenteur s'arrête brutalement.
 */
final class OutboxLock {

    static final String COLLECTION = "outbox_lock";
    private static final String LOCK_ID = "outbox-relay";

    private final MongoTemplate mongoTemplate;
    private final String owner;
    private final Duration lease;
    private final Clock clock;

    OutboxLock(MongoTemplate mongoTemplate, String owner, Duration lease, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.owner = owner;
        this.lease = lease;
        this.clock = clock;
    }

    /** Prend le verrou s'il est libre ou expiré, ou le prolonge si on le détient déjà. */
    boolean tryAcquire() {
        Instant now = clock.instant();
        Query freeOrMine = Query.query(where("_id").is(LOCK_ID)
                .orOperator(where("lockedUntil").lt(now), where("owner").is(owner)));
        try {
            // Si le verrou est détenu par un autre, l'upsert tente de créer un second document
            // avec le même _id : MongoDB le refuse (clé dupliquée).
            mongoTemplate.upsert(freeOrMine,
                    new Update().set("owner", owner).set("lockedUntil", now.plus(lease)), COLLECTION);
            return true;
        } catch (DuplicateKeyException heldByAnotherRelay) {
            return false;
        }
    }

    void release() {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(LOCK_ID).and("owner").is(owner)),
                new Update().set("lockedUntil", Instant.EPOCH), COLLECTION);
    }
}
