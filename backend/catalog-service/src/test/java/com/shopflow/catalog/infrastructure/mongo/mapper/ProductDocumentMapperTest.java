package com.shopflow.catalog.infrastructure.mongo.mapper;

import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.mongo.document.ProductDocument;
import com.shopflow.catalog.infrastructure.mongo.document.VariantDocument;
import com.shopflow.kernel.id.UuidV7;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversions domaine ↔ document, sans MongoDB ni Spring.
 */
class ProductDocumentMapperTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private final ProductDocumentMapper productDocumentMapper = new ProductDocumentMapperImpl();

    @Test
    @DisplayName("SHOULD copy every field WHEN mapping a product to a document")
    void shouldCopyEveryFieldWhenMappingProductToDocument() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", "Réédition 1990", NOW);

        // WHEN
        ProductDocument document = productDocumentMapper.toDocument(airMax);

        // THEN
        assertThat(document.getId()).isEqualTo(airMax.getId().toString());
        assertThat(document.getSellerId()).isEqualTo("seller-karim");
        assertThat(document.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(document.getBrand()).isEqualTo("Nike");
        assertThat(document.getCategory()).isEqualTo("sneakers");
        assertThat(document.getDescription()).isEqualTo("Réédition 1990");
        assertThat(document.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(document.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("SHOULD leave the description null WHEN the product has none")
    void shouldLeaveDescriptionNullWhenProductHasNone() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, NOW);

        // WHEN
        ProductDocument document = productDocumentMapper.toDocument(airMax);

        // THEN
        assertThat(document.getDescription()).isNull();
    }

    @Test
    @DisplayName("SHOULD rebuild the same product WHEN mapping a document back to the domain")
    void shouldRebuildSameProductWhenMappingDocumentBack() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, NOW);
        ProductDocument document = productDocumentMapper.toDocument(airMax);

        // WHEN
        Product rebuilt = productDocumentMapper.toDomain(document);

        // THEN
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(airMax);
        assertThat(rebuilt.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("SHOULD copy every variant field WHEN mapping a product with variants to a document")
    void shouldCopyEveryVariantFieldWhenMappingProductToDocument() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, NOW);
        Variant variant = airMax.addVariant("42", "Rouge", Money.euros(16_000));

        // WHEN
        ProductDocument document = productDocumentMapper.toDocument(airMax);

        // THEN
        assertThat(document.getVariants()).hasSize(1);
        VariantDocument variantDocument = document.getVariants().get(0);
        assertThat(variantDocument.getSku()).isEqualTo(variant.getSku().toString());
        assertThat(variantDocument.getSize()).isEqualTo("42");
        assertThat(variantDocument.getColor()).isEqualTo("Rouge");
        assertThat(variantDocument.getPriceAmount()).isEqualTo(16_000);
        assertThat(variantDocument.getPriceCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("SHOULD rebuild the same variants WHEN mapping a document back to the domain")
    void shouldRebuildSameVariantsWhenMappingDocumentBack() {
        // GIVEN
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, NOW);
        airMax.addVariant("42", "Rouge", Money.euros(16_000));
        airMax.addVariant("43", "Noir", Money.euros(17_000));
        ProductDocument document = productDocumentMapper.toDocument(airMax);

        // WHEN
        Product rebuilt = productDocumentMapper.toDomain(document);

        // THEN
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(airMax);
    }

    @Test
    @DisplayName("SHOULD have no variant WHEN the document was saved before variants existed")
    void shouldHaveNoVariantWhenDocumentWasSavedBeforeVariantsExisted() {
        // GIVEN
        ProductDocument document = ProductDocument.builder().id(UuidV7.generate().toString())
                .sellerId("seller-karim").name("Air Max 90").brand("Nike").category("sneakers")
                .status(ProductStatus.DRAFT).createdAt(NOW).build();

        // WHEN
        Product rebuilt = productDocumentMapper.toDomain(document);

        // THEN
        assertThat(rebuilt.getVariants()).isEmpty();
    }
}
