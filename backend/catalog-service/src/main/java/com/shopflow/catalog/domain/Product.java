package com.shopflow.catalog.domain;

import com.shopflow.kernel.money.Money;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Produit d'un vendeur (ex. « Air Max 90 Infrared » de Nike).
 * <p>
 * Aucune dépendance à Spring, MongoDB ou Kafka (Lombok n'agit qu'à la compilation). Un produit ne peut être créé
 * que par {@link #draft}. Le format des champs est vérifié à l'entrée des cas d'usage, par les annotations des
 * commandes : le domaine ne vérifie que ce qui dépend de son état.
 */
@Getter
public final class Product {

    public static final int NAME_MAX_LENGTH = 120;
    public static final int BRAND_MAX_LENGTH = 60;
    public static final int CATEGORY_MAX_LENGTH = 60;
    public static final int DESCRIPTION_MAX_LENGTH = 2_000;

    private final ProductId id;
    private final SellerId sellerId;
    private final String name;
    private final String brand;
    private final String category;

    /** Facultative : exposée par {@link #getDescription()} sous forme d'{@link Optional}. */
    @Getter(AccessLevel.NONE)
    private final String description;

    private final ProductStatus status;
    private final Instant createdAt;

    /** Modifiable uniquement par {@link #addVariant} : exposée par {@link #getVariants()} en lecture seule. */
    @Getter(AccessLevel.NONE)
    private final List<Variant> variants = new ArrayList<>();

    private Product(final ProductId id, final SellerId sellerId, final String name, final String brand,
                    final String category, final String description, final ProductStatus status,
                    final Instant createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Crée un brouillon, invisible des clients jusqu'à sa publication.
     *
     * @param description facultative
     * @param now         date de création (passée en paramètre pour que les tests puissent la fixer),
     *                    conservée à la milliseconde comme dans MongoDB
     */
    public static Product draft(final SellerId sellerId, final String name, final String brand, final String category,
                                final String description, final Instant now) {
        return new Product(ProductId.newId(), sellerId, name.strip(), brand.strip(), category.strip(),
                blankToNull(description), ProductStatus.DRAFT, now.truncatedTo(ChronoUnit.MILLIS));
    }

    /**
     * Reconstruit un produit enregistré. Les règles ne sont pas revérifiées : elles l'ont été à sa création.
     *
     * @param description {@code null} s'il n'y en a pas
     */
    public static Product restore(final ProductId id, final SellerId sellerId, final String name, final String brand,
                                  final String category, final String description, final ProductStatus status,
                                  final Instant createdAt) {
        return new Product(Objects.requireNonNull(id, "id"), Objects.requireNonNull(sellerId, "sellerId"),
                Objects.requireNonNull(name, "name"), Objects.requireNonNull(brand, "brand"),
                Objects.requireNonNull(category, "category"), description,
                Objects.requireNonNull(status, "status"), Objects.requireNonNull(createdAt, "createdAt"));
    }

    /**
     * Ajoute une variante au produit.
     *
     * @return la variante créée, avec son nouveau SKU
     * @throws DuplicateVariantException si le produit a déjà cette taille et ce coloris
     */
    public Variant addVariant(final String size, final String color, final Money price) {
        Variant variant = Variant.create(size, color, price);
        if (variants.stream().anyMatch(variant::hasSameOptionsAs)) {
            throw new DuplicateVariantException(variant);
        }
        variants.add(variant);
        return variant;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public List<Variant> getVariants() {
        return List.copyOf(variants);
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
