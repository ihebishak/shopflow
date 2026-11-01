package com.shopflow.catalog.infrastructure.kafka.publisher;

import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.kafka.mapper.ProductEventMapper;
import com.shopflow.starter.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptateur du port {@link ProductEventPublisher} : écrit l'événement dans l'outbox, dans la transaction en cours.
 * Le relais du starter le publie ensuite sur Kafka, avec l'identifiant du produit comme clé.
 */
@Component
@RequiredArgsConstructor
public class OutboxProductEventPublisher implements ProductEventPublisher {

    public static final String TOPIC = "shopflow.product.events";

    private final OutboxPublisher outboxPublisher;
    private final ProductEventMapper productEventMapper;

    @Override
    public void publishProductCreated(final Product product) {
        outboxPublisher.publish(TOPIC, product.getId().toString(), productEventMapper.toProductCreated(product));
    }

    @Override
    public void publishProductVariantAdded(final ProductId productId, final Variant variant) {
        outboxPublisher.publish(TOPIC, productId.toString(), productEventMapper.toProductVariantAdded(productId, variant));
    }
}
