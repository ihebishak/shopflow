package com.shopflow.starter.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Valeurs par défaut communes à tous les services, ajoutées avec la priorité la plus basse :
 * un service peut toujours les redéfinir dans son application.yml (ou par variable d'environnement).
 */
public class ShopflowDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "shopflowDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults()));
    }

    static Map<String, Object> defaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();

        // Logs : chaque ligne affiche l'identifiant de corrélation du parcours en cours
        defaults.put("logging.pattern.level", "%5p [%X{correlationId:-}]");

        // Producteur : clé = id de l'entité (texte), valeur = Avro ; aucun message perdu ni dupliqué par Kafka
        defaults.put("spring.kafka.producer.key-serializer", "org.apache.kafka.common.serialization.StringSerializer");
        defaults.put("spring.kafka.producer.value-serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        defaults.put("spring.kafka.producer.acks", "all");
        defaults.put("spring.kafka.producer.properties.enable.idempotence", "true");
        // Kafka injoignable : abandon en 10 s au lieu de 60 s (blocage) + 120 s (livraison), pour que l'outbox
        // ne garde jamais son verrou sur un envoi bloqué (cf. OutboxConfiguration#checkTimeouts)
        defaults.put("spring.kafka.producer.properties.max.block.ms", "10000");
        defaults.put("spring.kafka.producer.properties.request.timeout.ms", "5000");
        defaults.put("spring.kafka.producer.properties.delivery.timeout.ms", "10000");

        // Consommateur : un groupe par service, messages reçus sous forme de classes Avro générées
        defaults.put("spring.kafka.consumer.group-id", "${spring.application.name}");
        defaults.put("spring.kafka.consumer.auto-offset-reset", "earliest");
        defaults.put("spring.kafka.consumer.key-deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // Un message illisible ne fait pas planter la lecture : il part en DLT au lieu de bloquer le service
        defaults.put("spring.kafka.consumer.value-deserializer",
                "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        defaults.put("spring.kafka.consumer.properties.spring.deserializer.value.delegate.class",
                "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        defaults.put("spring.kafka.properties.specific.avro.reader", "true");

        // Schema Registry : un subject par type d'événement dans chaque topic (docs/03-conventions.md § 1.2)
        defaults.put("spring.kafka.properties.schema.registry.url", "http://localhost:8081");
        defaults.put("spring.kafka.properties.value.subject.name.strategy",
                "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy");
        // En local, le service enregistre ses schémas ; en CI/production, mettre false : seule la CI enregistre
        defaults.put("spring.kafka.properties.auto.register.schemas", "true");

        return defaults;
    }
}
