package com.shopflow.catalog.domain;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Produit d'un vendeur (ex. « Air Max 90 Infrared » de Nike).
 * <p>
 * Aucune dépendance à Spring, MongoDB ou Kafka (Lombok n'agit qu'à la compilation). Un produit ne peut être créé
 * que par {@link #draft}, qui garantit qu'il respecte les règles métier.
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
     * @throws InvalidProductException si une règle n'est pas respectée
     */
    public static Product draft(final SellerId sellerId, final String name, final String brand, final String category,
                                final String description, final Instant now) {
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(now, "now");
        return new Product(ProductId.newId(), sellerId,
                required("name", name, NAME_MAX_LENGTH),
                required("brand", brand, BRAND_MAX_LENGTH),
                required("category", category, CATEGORY_MAX_LENGTH),
                optional("description", description, DESCRIPTION_MAX_LENGTH),
                ProductStatus.DRAFT, now.truncatedTo(ChronoUnit.MILLIS));
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

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    private static String required(final String field, final String value, final int maxLength) {
        String text = value == null ? "" : value.strip();
        if (text.isEmpty()) {
            throw new InvalidProductException(field, field + " is required");
        }
        return checkLength(field, text, maxLength);
    }

    private static String optional(final String field, final String value, final int maxLength) {
        String text = value == null ? "" : value.strip();
        return text.isEmpty() ? null : checkLength(field, text, maxLength);
    }

    private static String checkLength(final String field, final String text, final int maxLength) {
        if (text.length() > maxLength) {
            throw new InvalidProductException(field, field + " must be at most " + maxLength + " characters");
        }
        return text;
    }
}
