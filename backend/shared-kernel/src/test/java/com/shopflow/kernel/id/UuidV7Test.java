package com.shopflow.kernel.id;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidV7Test {

    @Test
    void has_version_7_and_rfc_variant() {
        UUID uuid = UuidV7.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void embeds_its_creation_timestamp() {
        long millis = Instant.parse("2026-10-28T10:00:00.123Z").toEpochMilli();

        assertThat(UuidV7.timestampOf(UuidV7.generate(millis))).isEqualTo(Instant.ofEpochMilli(millis));
    }

    @Test
    void is_ordered_in_time() {
        UUID earlier = UuidV7.generate(1_000);
        UUID later = UuidV7.generate(2_000);

        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    void is_strictly_increasing_even_within_the_same_millisecond() {
        String previous = UuidV7.generate().toString();
        for (int i = 0; i < 100_000; i++) {
            String next = UuidV7.generate().toString();
            assertThat(next).isGreaterThan(previous);
            previous = next;
        }
    }

    @Test
    void is_unique() {
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            uuids.add(UuidV7.generate());
        }

        assertThat(uuids).hasSize(10_000);
    }

    @Test
    void refuses_to_read_the_timestamp_of_another_version() {
        assertThatThrownBy(() -> UuidV7.timestampOf(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
