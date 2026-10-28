package com.shopflow.starter.cloudevents;

import org.apache.avro.Schema;

/**
 * Calcule le {@code ce_type} à partir du schéma Avro :
 * {@code com.shopflow.contracts.product.ProductCreated} → {@code shopflow.product.ProductCreated.v1}.
 * La version vient de la propriété {@code "version"} du schéma (1 si absente).
 */
public final class CloudEventTypes {

    static final String CONTRACTS_NAMESPACE = "com.shopflow.contracts.";
    static final String VERSION_PROPERTY = "version";

    private CloudEventTypes() {
    }

    public static String typeOf(Schema schema) {
        String namespace = schema.getNamespace();
        if (namespace == null || !namespace.startsWith(CONTRACTS_NAMESPACE)) {
            throw new IllegalArgumentException("Not a ShopFlow contract: " + schema.getFullName());
        }
        String context = namespace.substring(CONTRACTS_NAMESPACE.length());
        String version = schema.getProp(VERSION_PROPERTY);
        return "shopflow." + context + "." + schema.getName() + ".v" + (version == null ? "1" : version);
    }
}
