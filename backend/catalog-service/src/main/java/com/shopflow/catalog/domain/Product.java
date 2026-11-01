package com.shopflow.catalog.domain;

import com.shopflow.kernel.money.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
 * <p>
 * Immuable : un changement renvoie un nouveau produit, qu'il faut récupérer pour l'enregistrer.
 */
@Getter
@Builder(toBuilder = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    /** Toujours une liste non modifiable : {@code getVariants()} peut l'exposer telle quelle. */
    private final List<Variant> variants;

    /** Numéro de l'enregistrement lu, {@code null} avant le premier : empêche d'écraser une modification concurrente. */
    private final Long version;

    /**
     * Crée un brouillon, invisible des clients jusqu'à sa publication.
     *
     * @param description facultative
     * @param now         date de création (passée en paramètre pour que les tests puissent la fixer),
     *                    conservée à la milliseconde comme dans MongoDB
     */
    public static Product draft(final SellerId sellerId, final String name, final String brand, final String category,
                                final String description, final Instant now) {
        return Product.builder()
                .id(ProductId.newId())
                .sellerId(sellerId)
                .name(name.strip())
                .brand(brand.strip())
                .category(category.strip())
                .description(blankToNull(description))
                .status(ProductStatus.DRAFT)
                .createdAt(now.truncatedTo(ChronoUnit.MILLIS))
                .variants(List.of())
                .build();
    }

    /**
     * Reconstruit un produit enregistré. Les règles ne sont pas revérifiées : elles l'ont été à sa création.
     *
     * @param description {@code null} s'il n'y en a pas
     * @param version     numéro de l'enregistrement lu
     */
    public static Product restore(final ProductId id, final SellerId sellerId, final String name, final String brand,
                                  final String category, final String description, final ProductStatus status,
                                  final Instant createdAt, final List<Variant> variants, final Long version) {
        return Product.builder()
                .id(Objects.requireNonNull(id, "id"))
                .sellerId(Objects.requireNonNull(sellerId, "sellerId"))
                .name(Objects.requireNonNull(name, "name"))
                .brand(Objects.requireNonNull(brand, "brand"))
                .category(Objects.requireNonNull(category, "category"))
                .description(description)
                .status(Objects.requireNonNull(status, "status"))
                .createdAt(Objects.requireNonNull(createdAt, "createdAt"))
                .variants(List.copyOf(Objects.requireNonNull(variants, "variants")))
                .version(version)
                .build();
    }

    /**
     * Ajoute une variante au produit.
     *
     * @return le produit avec cette variante
     * @throws DuplicateVariantException si le produit a déjà cette taille et ce coloris
     */
    public Product addVariant(final Variant variant) {
        if (variants.stream().anyMatch(variant::hasSameOptionsAs)) {
            throw new DuplicateVariantException(variant);
        }
        List<Variant> newVariants = new ArrayList<>(variants);
        newVariants.add(variant);
        return toBuilder().variants(List.copyOf(newVariants)).build();
    }

    /**
     * @return la variante du produit portant ce SKU
     * @throws VariantNotFoundException si le produit n'a pas cette variante
     */
    public Variant variant(final Sku sku) {
        return variants.stream().filter(variant -> variant.getSku().equals(sku)).findFirst()
                .orElseThrow(() -> new VariantNotFoundException(id, sku));
    }

    /**
     * Change le prix d'une variante, qui garde sa place parmi les autres.
     *
     * @return le produit avec ce prix
     * @throws VariantNotFoundException si le produit n'a pas cette variante
     */
    public Product changeVariantPrice(final Sku sku, final Money newPrice) {
        Variant changed = variant(sku).withPrice(newPrice);
        List<Variant> newVariants = variants.stream()
                .map(variant -> variant.getSku().equals(sku) ? changed : variant).toList();
        return toBuilder().variants(newVariants).build();
    }

    /**
     * Modifie les informations du produit, nettoyées comme à la création.
     *
     * @param description facultative : l'omettre l'efface
     * @return le produit avec ces informations
     */
    public Product update(final String name, final String brand, final String category, final String description) {
        return toBuilder().name(name.strip()).brand(brand.strip()).category(category.strip())
                .description(blankToNull(description)).build();
    }

    /**
     * @param before le produit avant sa modification
     * @return les noms des informations qui diffèrent, vide si rien n'a changé
     */
    public List<String> changedFieldsSince(final Product before) {
        List<String> changedFields = new ArrayList<>();
        addIfChanged(changedFields, "name", before.name, name);
        addIfChanged(changedFields, "brand", before.brand, brand);
        addIfChanged(changedFields, "category", before.category, category);
        addIfChanged(changedFields, "description", before.description, description);
        return List.copyOf(changedFields);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    private static void addIfChanged(final List<String> changedFields, final String field, final String oldValue,
                                     final String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changedFields.add(field);
        }
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
