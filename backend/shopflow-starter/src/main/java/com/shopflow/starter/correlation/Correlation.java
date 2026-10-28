package com.shopflow.starter.correlation;

import com.shopflow.kernel.id.UuidV7;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Identifiant de corrélation du parcours en cours : suit un parcours métier de bout en bout
 * (HTTP, Kafka, Camunda, logs). Stocké dans le MDC pour apparaître sur chaque ligne de log.
 */
public final class Correlation {

    public static final String MDC_KEY = "correlationId";
    public static final String HTTP_HEADER = "X-Correlation-Id";

    /** Valeurs acceptées depuis l'extérieur : évite l'injection de contenu arbitraire dans les logs. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private Correlation() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY)).filter(id -> !id.isBlank());
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static String newId() {
        return UuidV7.generate().toString();
    }

    /** Retourne l'identifiant reçu s'il est valide, sinon en génère un nouveau. */
    public static String sanitizeOrNew(String candidate) {
        return candidate != null && VALID_ID.matcher(candidate).matches() ? candidate : newId();
    }
}
