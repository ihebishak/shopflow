package com.shopflow.catalog.domain;

import com.shopflow.kernel.money.Money;
import lombok.Getter;

import java.util.Objects;

/**
 * Déclinaison achetable d'un produit (ex. « Air Max 90 Infrared, 42, Rouge, 160 € »).
 * <p>
 * Comme pour {@link Product}, le format des champs est vérifié à l'entrée des cas d'usage. Une variante ne peut être
 * créée que par {@link Product#addVariant}.
 */
@Getter
public final class Variant {

    public static final int SIZE_MAX_LENGTH = 20;
    public static final int COLOR_MAX_LENGTH = 40;

    private final Sku sku;
    private final String size;
    private final String color;
    private final Money price;

    private Variant(final Sku sku, final String size, final String color, final Money price) {
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.price = price;
    }

    static Variant create(final String size, final String color, final Money price) {
        return new Variant(Sku.newSku(), size.strip(), color.strip(), price);
    }

    /** Reconstruit une variante enregistrée. Les règles ne sont pas revérifiées : elles l'ont été à sa création. */
    public static Variant restore(final Sku sku, final String size, final String color, final Money price) {
        return new Variant(Objects.requireNonNull(sku, "sku"), Objects.requireNonNull(size, "size"),
                Objects.requireNonNull(color, "color"), Objects.requireNonNull(price, "price"));
    }

    /** Même taille et même coloris, sans tenir compte de la casse (« rouge » et « Rouge » sont le même coloris). */
    boolean hasSameOptionsAs(final Variant other) {
        return size.equalsIgnoreCase(other.size) && color.equalsIgnoreCase(other.color);
    }
}
