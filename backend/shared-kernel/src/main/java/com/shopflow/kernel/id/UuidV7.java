package com.shopflow.kernel.id;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Générateur d'UUID version 7 (RFC 9562) : 48 bits de timestamp en millisecondes, un compteur de 12 bits,
 * puis de l'aléatoire. Uniques comme des UUID classiques, mais ordonnés dans le temps.
 * <p>
 * Les identifiants générés par une même JVM sont <b>strictement croissants</b>, même dans une même
 * milliseconde (le compteur est incrémenté) : l'outbox s'appuie dessus pour publier dans l'ordre.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int COUNTER_LIMIT = 1 << 12;

    private static long lastMillis = -1;
    private static int counter;

    private UuidV7() {
    }

    public static synchronized UUID generate() {
        long now = System.currentTimeMillis();
        if (now > lastMillis) {
            lastMillis = now;
            // Démarre dans la moitié basse pour laisser de la place aux incréments de la même milliseconde
            counter = RANDOM.nextInt(COUNTER_LIMIT / 2);
        } else if (++counter == COUNTER_LIMIT) {
            // Même milliseconde (ou horloge revenue en arrière) et compteur épuisé : on avance d'une milliseconde
            lastMillis++;
            counter = 0;
        }
        return build(lastMillis, counter);
    }

    static UUID generate(long epochMillis) {
        return build(epochMillis, RANDOM.nextInt(COUNTER_LIMIT));
    }

    private static UUID build(long epochMillis, int counter) {
        long mostSigBits = (epochMillis & 0xFFFF_FFFF_FFFFL) << 16  // unix_ts_ms (48 bits)
                | 0x7000L                                           // version 7 (4 bits)
                | counter;                                          // compteur (12 bits)
        long leastSigBits = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) // aléatoire (62 bits)
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
