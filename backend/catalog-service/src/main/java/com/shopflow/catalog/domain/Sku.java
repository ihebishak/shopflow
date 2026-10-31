package com.shopflow.catalog.domain;

import com.shopflow.kernel.id.UuidV7;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifiant d'une variante (UUID v7), généré par le serveur. Un type dédié empêche de le confondre avec un autre
 * identifiant.
 */
@Getter
@EqualsAndHashCode
public final class Sku {

    private final UUID value;

    public Sku(final UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static Sku newSku() {
        return new Sku(UuidV7.generate());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
