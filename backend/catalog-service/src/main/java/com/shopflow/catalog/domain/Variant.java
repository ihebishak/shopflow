package com.shopflow.catalog.domain;

import com.shopflow.kernel.money.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

/**
 * Déclinaison achetable d'un produit (ex. « Air Max 90 Infrared, 42, Rouge, 160 € »).
 * <p>
 * Comme pour {@link Product}, le format des champs est vérifié à l'entrée des cas d'usage. Une variante créée
 * n'appartient à un produit qu'une fois passée à {@link Product#addVariant}.
 * <p>
 * Immuable : changer le prix renvoie une nouvelle variante, comme pour {@link Product}.
 */
@Getter
@Builder(toBuilder = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variant {

    public static final int SIZE_MAX_LENGTH = 20;
    public static final int COLOR_MAX_LENGTH = 40;

    private final Sku sku;
    private final String size;
    private final String color;
    private final Money price;

    /** Crée une variante avec un nouveau SKU. */
    public static Variant create(final String size, final String color, final Money price) {
        return new Variant(Sku.newSku(), size.strip(), color.strip(), price);
    }

    /** Reconstruit une variante enregistrée. Les règles ne sont pas revérifiées : elles l'ont été à sa création. */
    public static Variant restore(final Sku sku, final String size, final String color, final Money price) {
        return new Variant(Objects.requireNonNull(sku, "sku"), Objects.requireNonNull(size, "size"),
                Objects.requireNonNull(color, "color"), Objects.requireNonNull(price, "price"));
    }

    /** La même variante, à ce prix. Passer par {@link Product#changeVariantPrice} pour l'enregistrer. */
    Variant withPrice(final Money newPrice) {
        return toBuilder().price(newPrice).build();
    }

    /** Même taille et même coloris, sans tenir compte de la casse (« rouge » et « Rouge » sont le même coloris). */
    boolean hasSameOptionsAs(final Variant other) {
        return size.equalsIgnoreCase(other.size) && color.equalsIgnoreCase(other.color);
    }
}
