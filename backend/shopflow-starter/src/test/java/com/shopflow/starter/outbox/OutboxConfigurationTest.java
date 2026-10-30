package com.shopflow.starter.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérification au démarrage : un envoi bloqué ne doit jamais survivre au verrou du relais.
 */
class OutboxConfigurationTest {

    private static final Map<String, Object> STARTER_DEFAULTS =
            Map.of("max.block.ms", "10000", "delivery.timeout.ms", "10000");

    @Test
    void accepts_the_starter_defaults() {
        assertThatCode(() -> OutboxConfiguration.checkTimeouts(properties("PT15S", "PT30S"), STARTER_DEFAULTS))
                .doesNotThrowAnyException();
    }

    @Test
    void refuses_a_relay_that_gives_up_before_kafka() {
        assertThatThrownBy(() -> OutboxConfiguration.checkTimeouts(properties("PT10S", "PT30S"), STARTER_DEFAULTS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivery.timeout.ms");
    }

    @Test
    void refuses_a_lock_shorter_than_the_longest_send() {
        assertThatThrownBy(() -> OutboxConfiguration.checkTimeouts(properties("PT15S", "PT20S"), STARTER_DEFAULTS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lock-lease");
    }

    @Test
    void refuses_the_kafka_defaults_of_60_and_120_seconds() {
        assertThatThrownBy(() -> OutboxConfiguration.checkTimeouts(properties("PT15S", "PT30S"), Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivery.timeout.ms (PT2M)");
    }

    private static OutboxProperties properties(String sendTimeout, String lockLease) {
        return new OutboxProperties(Duration.ofSeconds(1), 100, Duration.parse(lockLease), Duration.parse(sendTimeout),
                Duration.ofDays(7));
    }
}
