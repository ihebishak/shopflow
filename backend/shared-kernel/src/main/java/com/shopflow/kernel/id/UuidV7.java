package com.shopflow.kernel.id;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Générateur d'UUID version 7 (RFC 9562) : 48 bits de timestamp en millisecondes, puis de l'aléatoire.
 * Uniques comme des UUID classiques, mais ordonnés dans le temps (efficaces pour les index MongoDB).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    static UUID generate(long epochMillis) {
        long mostSigBits = (epochMillis & 0xFFFF_FFFF_FFFFL) << 16  // unix_ts_ms (48 bits)
                | 0x7000L                                           // version 7 (4 bits)
                | RANDOM.nextInt(1 << 12);                          // rand_a (12 bits)
        long leastSigBits = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) // rand_b (62 bits)
                | 0x8000_0000_0000_0000L;                           // variante RFC (2 bits)
        return new UUID(mostSigBits, leastSigBits);
    }

    public static Instant timestampOf(UUID uuid) {
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("Not a UUIDv7: " + uuid);
        }
        return Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }
}
