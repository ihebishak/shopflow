package com.shopflow.starter.outbox;

import com.shopflow.starter.cloudevents.EventMetadata;
import com.shopflow.starter.cloudevents.EventMetadataFactory;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Point d'entrée des services pour publier un événement ou une commande.
 * <p>
 * Le message est écrit dans la collection {@code outbox}, <b>dans la transaction MongoDB en cours</b> :
 * il est enregistré si et seulement si les changements métier le sont. Le {@link OutboxRelay} le publie
 * ensuite sur Kafka (réveillé dès la validation de la transaction).
 */
public class OutboxPublisher {

    private final MongoTemplate mongoTemplate;
    private final EventMetadataFactory metadataFactory;
    private final OutboxRelay relay;

    public OutboxPublisher(MongoTemplate mongoTemplate, EventMetadataFactory metadataFactory, OutboxRelay relay) {
        this.mongoTemplate = mongoTemplate;
        this.metadataFactory = metadataFactory;
        this.relay = relay;
    }

    public void publish(String topic, String key, SpecificRecord message) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "OutboxPublisher.publish must be called inside a transaction (@Transactional): " +
                    "otherwise the event could be lost or published for changes that were never saved");
        }
        EventMetadata metadata = metadataFactory.create(key, message);
        mongoTemplate.insert(OutboxMessage.pending(topic, key, message.getSchema().getFullName(),
                AvroJsonCodec.encode(message), metadata));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                relay.wakeUp();
            }
        });
    }
}
