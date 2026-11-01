package com.shopflow.catalog.domain;

import com.shopflow.kernel.error.ErrorKind;
import com.shopflow.kernel.id.UuidV7;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Règles d'un produit, sans Spring ni base de données : chaque test s'exécute en quelques millisecondes.
 */
class ProductTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final Money PRICE = Money.euros(16_000);

    @Test
    @DisplayName("SHOULD create a draft with a new id and trimmed fields WHEN the product is valid")
    void shouldCreateDraftWhenProductIsValid() {
        // GIVEN
        String name = "  Air Max 90 Infrared ";
        String description = "  La réédition du modèle de 1990  ";

        // WHEN
        Product product = Product.draft(KARIM, name, " Nike", "sneakers ", description, NOW);

        // THEN
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getSellerId()).isEqualTo(KARIM);
        assertThat(product.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.getBrand()).isEqualTo("Nike");
        assertThat(product.getCategory()).isEqualTo("sneakers");
        assertThat(product.getDescription()).contains("La réédition du modèle de 1990");
        assertThat(product.getCreatedAt()).isEqualTo(NOW);
        assertThat(product.getVersion()).isNull();
        assertThat(product.getId().getValue().version()).as("UUID v7").isEqualTo(7);
        assertThat(UuidV7.timestampOf(product.getId().getValue())).isNotNull();
    }

    @Test
    @DisplayName("SHOULD keep the creation date to the millisecond WHEN the clock is more precise (like MongoDB)")
    void shouldKeepCreationDateToTheMillisecondWhenClockIsMorePrecise() {
        // GIVEN
        Instant preciseNow = Instant.parse("2026-10-31T10:00:00.123456789Z");

        // WHEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", null, preciseNow);

        // THEN
        assertThat(product.getCreatedAt()).isEqualTo(Instant.parse("2026-10-31T10:00:00.123Z"));
    }

    @Test
    @DisplayName("SHOULD give each product its own id WHEN two identical products are created")
    void shouldGiveEachProductItsOwnIdWhenTwoIdenticalProductsAreCreated() {
        // GIVEN / WHEN
        Product first = draftNamed("Air Max 90");
        Product second = draftNamed("Air Max 90");

        // THEN
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD have no description WHEN the description is missing or blank")
    void shouldHaveNoDescriptionWhenDescriptionIsMissingOrBlank(final String description) {
        // WHEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", description, NOW);

        // THEN
        assertThat(product.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD refuse the seller id WHEN it is blank")
    void shouldRefuseSellerIdWhenBlank() {
        // WHEN
        Throwable error = catchThrowable(() -> new SellerId(" "));

        // THEN
        assertThat(error).isInstanceOf(InvalidSellerIdException.class).hasMessage("Seller id must not be blank");
        assertThat(((InvalidSellerIdException) error).getCode()).isEqualTo("INVALID_SELLER_ID");
    }

    @Test
    @DisplayName("SHOULD have no variant WHEN the product is created")
    void shouldHaveNoVariantWhenProductIsCreated() {
        // WHEN
        Product product = draftNamed("Air Max 90");

        // THEN
        assertThat(product.getVariants()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD add a variant with a new SKU and trimmed options WHEN the variant is valid")
    void shouldAddVariantWhenVariantIsValid() {
        // GIVEN
        Product product = draftNamed("Air Max 90");
        Variant variant = Variant.create(" 42 ", " Rouge ", PRICE);

        // WHEN
        Product withVariant = product.addVariant(variant);

        // THEN
        assertThat(withVariant.getVariants()).containsExactly(variant);
        assertThat(variant.getSize()).isEqualTo("42");
        assertThat(variant.getColor()).isEqualTo("Rouge");
        assertThat(variant.getPrice()).isEqualTo(PRICE);
        assertThat(variant.getSku().getValue().version()).as("UUID v7").isEqualTo(7);
    }

    @Test
    @DisplayName("SHOULD give each variant its own SKU WHEN two variants are created")
    void shouldGiveEachVariantItsOwnSkuWhenTwoVariantsAreCreated() {
        // WHEN
        Variant first = Variant.create("42", "Rouge", PRICE);
        Variant second = Variant.create("43", "Rouge", PRICE);

        // THEN
        assertThat(first.getSku()).isNotEqualTo(second.getSku());
    }

    @ParameterizedTest
    @CsvSource({"43, Rouge", "42, Noir"})
    @DisplayName("SHOULD add the variant WHEN its size or its color is different")
    void shouldAddVariantWhenSizeOrColorIsDifferent(final String size, final String color) {
        // GIVEN
        Product product = draftNamed("Air Max 90").addVariant(Variant.create("42", "Rouge", PRICE));

        // WHEN
        Product withBoth = product.addVariant(Variant.create(size, color, PRICE));

        // THEN
        assertThat(withBoth.getVariants()).hasSize(2);
    }

    @ParameterizedTest
    @CsvSource({"42, Rouge", "42, rouge", "42, ROUGE", "xl, Rouge", "' 42 ', ' Rouge '"})
    @DisplayName("SHOULD refuse the variant WHEN its size and its color already exist, whatever the case")
    void shouldRefuseVariantWhenSizeAndColorAlreadyExist(final String size, final String color) {
        // GIVEN
        Product product = draftNamed("Air Max 90")
                .addVariant(Variant.create("42", "Rouge", PRICE))
                .addVariant(Variant.create("XL", "Rouge", PRICE));

        // WHEN
        Throwable error = catchThrowable(() -> product.addVariant(Variant.create(size, color, PRICE)));

        // THEN
        assertThat(error).isInstanceOf(DuplicateVariantException.class)
                .hasMessage("Variant " + size.strip() + " / " + color.strip() + " already exists");
        assertThat(((DuplicateVariantException) error).getKind()).isEqualTo(ErrorKind.CONFLICT);
        assertThat(((DuplicateVariantException) error).getCode()).isEqualTo("DUPLICATE_VARIANT");
        assertThat(product.getVariants()).hasSize(2);
    }

    @Test
    @DisplayName("SHOULD refuse any change WHEN the variants are modified from the outside")
    void shouldRefuseChangeWhenVariantsAreModifiedFromTheOutside() {
        // GIVEN
        Variant variant = Variant.create("42", "Rouge", PRICE);
        Product product = draftNamed("Air Max 90").addVariant(variant);

        // WHEN
        Throwable error = catchThrowable(() -> product.getVariants().add(variant));

        // THEN
        assertThat(error).isInstanceOf(UnsupportedOperationException.class);
        assertThat(product.getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("SHOULD add a variant WHEN the product has been restored")
    void shouldAddVariantWhenProductHasBeenRestored() {
        // GIVEN
        Product product = restoredWith(Variant.restore(Sku.newSku(), "42", "Rouge", PRICE));

        // WHEN
        Product withBoth = product.addVariant(Variant.create("43", "Rouge", PRICE));

        // THEN
        assertThat(withBoth.getVariants()).hasSize(2);
    }

    @Test
    @DisplayName("SHOULD keep its variants WHEN the list given to restore is modified afterwards")
    void shouldKeepVariantsWhenListGivenToRestoreIsModified() {
        // GIVEN
        List<Variant> variants = new ArrayList<>(List.of(Variant.restore(Sku.newSku(), "42", "Rouge", PRICE)));
        Product product = Product.restore(ProductId.newId(), KARIM, "Air Max 90", "Nike", "sneakers", null,
                ProductStatus.DRAFT, NOW, variants, 0L);

        // WHEN
        variants.clear();

        // THEN
        assertThat(product.getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("SHOULD refuse the variant WHEN the restored product already has it")
    void shouldRefuseVariantWhenRestoredProductAlreadyHasIt() {
        // GIVEN
        Product product = restoredWith(Variant.restore(Sku.newSku(), "42", "Rouge", PRICE));

        // WHEN
        Throwable error = catchThrowable(() -> product.addVariant(Variant.create("42", "rouge", PRICE)));

        // THEN
        assertThat(error).isInstanceOf(DuplicateVariantException.class);
    }

    @Test
    @DisplayName("SHOULD keep every other field and leave the original untouched WHEN a variant is added")
    void shouldKeepEverythingElseWhenVariantIsAdded() {
        // GIVEN
        Variant existing = Variant.restore(Sku.newSku(), "42", "Rouge", PRICE);
        Product product = restoredWith(existing);
        Variant added = Variant.create("43", "Rouge", PRICE);

        // WHEN
        Product withBoth = product.addVariant(added);

        // THEN
        assertThat(withBoth).usingRecursiveComparison().ignoringFields("variants").isEqualTo(product);
        assertThat(withBoth.getVariants()).containsExactly(existing, added);
        assertThat(product.getVariants()).containsExactly(existing);
    }

    @Test
    @DisplayName("SHOULD change only the description and report it WHEN only the description differs")
    void shouldReportDescriptionWhenOnlyDescriptionDiffers() {
        // GIVEN
        Product product = airMax();

        // WHEN
        Product updated = product.update("Air Max 90", "Nike", "sneakers", "La réédition de 1990");

        // THEN
        assertThat(updated.changedFieldsSince(product)).containsExactly("description");
        assertThat(updated.getDescription()).contains("La réédition de 1990");
        assertThat(updated.getName()).isEqualTo("Air Max 90");
        assertThat(updated.getBrand()).isEqualTo("Nike");
        assertThat(updated.getCategory()).isEqualTo("sneakers");
    }

    @Test
    @DisplayName("SHOULD change every field and report them all WHEN every field differs")
    void shouldReportEveryFieldWhenEveryFieldDiffers() {
        // GIVEN
        Product product = airMax();

        // WHEN
        Product updated = product.update("Stan Smith", "Adidas", "tennis", "Un classique");

        // THEN
        assertThat(updated.changedFieldsSince(product)).containsExactly("name", "brand", "category", "description");
        assertThat(updated.getName()).isEqualTo("Stan Smith");
        assertThat(updated.getBrand()).isEqualTo("Adidas");
        assertThat(updated.getCategory()).isEqualTo("tennis");
        assertThat(updated.getDescription()).contains("Un classique");
    }

    @ParameterizedTest
    @CsvSource({
            "Stan Smith, Nike, sneakers, Réédition, name",
            "Air Max 90, Adidas, sneakers, Réédition, brand",
            "Air Max 90, Nike, tennis, Réédition, category"
    })
    @DisplayName("SHOULD report only the changed field WHEN one field differs")
    void shouldReportOnlyChangedFieldWhenOneFieldDiffers(final String name, final String brand, final String category,
                                                         final String description, final String changedField) {
        // GIVEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "Réédition", NOW);

        // WHEN
        Product updated = product.update(name, brand, category, description);

        // THEN
        assertThat(updated.changedFieldsSince(product)).containsExactly(changedField);
    }

    @Test
    @DisplayName("SHOULD report nothing WHEN nothing differs")
    void shouldReportNothingWhenNothingDiffers() {
        // GIVEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "Réédition", NOW);

        // WHEN
        Product updated = product.update("Air Max 90", "Nike", "sneakers", "Réédition");

        // THEN
        assertThat(updated.changedFieldsSince(product)).isEmpty();
    }

    @Test
    @DisplayName("SHOULD strip the fields and report nothing WHEN only surrounding spaces differ")
    void shouldReportNothingWhenOnlySurroundingSpacesDiffer() {
        // GIVEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "Réédition", NOW);

        // WHEN
        Product updated = product.update("  Air Max 90 ", " Nike", "sneakers ", "  Réédition  ");

        // THEN
        assertThat(updated.changedFieldsSince(product)).isEmpty();
        assertThat(updated.getName()).isEqualTo("Air Max 90");
        assertThat(updated.getBrand()).isEqualTo("Nike");
        assertThat(updated.getCategory()).isEqualTo("sneakers");
        assertThat(updated.getDescription()).contains("Réédition");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("SHOULD erase the description and report it WHEN the new description is missing or blank")
    void shouldEraseDescriptionWhenNewDescriptionIsMissingOrBlank(final String description) {
        // GIVEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "Réédition", NOW);

        // WHEN
        Product updated = product.update("Air Max 90", "Nike", "sneakers", description);

        // THEN
        assertThat(updated.changedFieldsSince(product)).containsExactly("description");
        assertThat(updated.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD report nothing WHEN the product still has no description")
    void shouldReportNothingWhenProductStillHasNoDescription() {
        // GIVEN
        Product product = airMax();

        // WHEN
        Product updated = product.update("Air Max 90", "Nike", "sneakers", "  ");

        // THEN
        assertThat(updated.changedFieldsSince(product)).isEmpty();
    }

    @Test
    @DisplayName("SHOULD keep the identity, the status, the date, the variants and the version WHEN the product is updated")
    void shouldKeepEverythingElseWhenProductIsUpdated() {
        // GIVEN
        Variant variant = Variant.restore(Sku.newSku(), "42", "Rouge", PRICE);
        Product product = restoredWith(variant);

        // WHEN
        Product updated = product.update("Stan Smith", "Adidas", "tennis", "Un classique");

        // THEN
        assertThat(updated.getId()).isEqualTo(product.getId());
        assertThat(updated.getSellerId()).isEqualTo(KARIM);
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(updated.getCreatedAt()).isEqualTo(NOW);
        assertThat(updated.getVariants()).containsExactly(variant);
        assertThat(updated.getVersion()).isZero();
    }

    @Test
    @DisplayName("SHOULD leave the original untouched WHEN the product is updated")
    void shouldLeaveOriginalUntouchedWhenProductIsUpdated() {
        // GIVEN
        Product product = Product.draft(KARIM, "Air Max 90", "Nike", "sneakers", "Réédition", NOW);

        // WHEN
        product.update("Stan Smith", "Adidas", "tennis", "Un classique");

        // THEN
        assertThat(product.getName()).isEqualTo("Air Max 90");
        assertThat(product.getBrand()).isEqualTo("Nike");
        assertThat(product.getCategory()).isEqualTo("sneakers");
        assertThat(product.getDescription()).contains("Réédition");
    }

    private static Product airMax() {
        return draftNamed("Air Max 90");
    }

    private static Product restoredWith(final Variant... variants) {
        return Product.restore(ProductId.newId(), KARIM, "Air Max 90", "Nike", "sneakers", null, ProductStatus.DRAFT,
                NOW, List.of(variants), 0L);
    }

    private static Product draftNamed(final String name) {
        return Product.draft(KARIM, name, "Nike", "sneakers", null, NOW);
    }
}
