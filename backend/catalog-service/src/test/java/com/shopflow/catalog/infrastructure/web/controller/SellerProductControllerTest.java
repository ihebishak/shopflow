package com.shopflow.catalog.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.application.api.AddVariantUseCase;
import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.DuplicateVariantException;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.ProductNotFoundException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Sku;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.MoneyRequestDto;
import com.shopflow.catalog.infrastructure.web.mapper.ProductRestMapperImpl;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrôleur seul (couche HTTP) : les cas d'usage sont simulés, ni MongoDB ni Kafka.
 * Le gestionnaire d'erreurs du starter est actif : les réponses d'erreur sont celles du vrai service.
 */
@WebMvcTest(SellerProductController.class)
@Import(ProductRestMapperImpl.class)
class SellerProductControllerTest {

    private static final String PRODUCTS = "/api/v1/seller/products";
    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final String AIR_MAX_JSON = """
            { "name": "Air Max 90 Infrared", "brand": "Nike", "category": "sneakers" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private GetSellerProductUseCase getSellerProductUseCase;

    @MockitoBean
    private AddVariantUseCase addVariantUseCase;

    @Test
    @DisplayName("SHOULD return 201 with the location and the product WHEN the product is valid")
    void shouldReturn201WhenProductIsValid() throws Exception {
        // GIVEN
        Product airMax = airMaxOf(KARIM);
        when(createProductUseCase.createProduct(any(CreateProductCommand.class))).thenReturn(airMax);

        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).header(SellerProductController.SELLER_HEADER, "seller-karim")
                        .contentType(MediaType.APPLICATION_JSON).content(AIR_MAX_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + PRODUCTS + "/" + airMax.getId()))
                .andExpect(jsonPath("$.id").value(airMax.getId().toString()))
                .andExpect(jsonPath("$.sellerId").value("seller-karim"))
                .andExpect(jsonPath("$.name").value("Air Max 90 Infrared"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field, without calling the use case, WHEN the name is blank")
    void shouldReturn400WithFieldWhenNameIsBlank() throws Exception {
        // GIVEN
        String blankName = """
                { "name": " ", "brand": "Nike", "category": "sneakers" }
                """;

        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).header(SellerProductController.SELLER_HEADER, "seller-karim")
                        .contentType(MediaType.APPLICATION_JSON).content(blankName))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.errors[0].message").value("name is required"));
        verify(createProductUseCase, never()).createProduct(any());
    }

    @Test
    @DisplayName("SHOULD return 400 MISSING_HEADER WHEN the seller header is missing")
    void shouldReturn400WhenSellerHeaderIsMissing() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(AIR_MAX_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
        verify(createProductUseCase, never()).createProduct(any());
    }

    @Test
    @DisplayName("SHOULD return 400 INVALID_SELLER_ID WHEN the seller header is blank")
    void shouldReturn400WhenSellerHeaderIsBlank() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).header(SellerProductController.SELLER_HEADER, "   ")
                        .contentType(MediaType.APPLICATION_JSON).content(AIR_MAX_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SELLER_ID"));
        verify(createProductUseCase, never()).createProduct(any());
    }

    @Test
    @DisplayName("SHOULD return 200 with the product WHEN the seller reads one of its products")
    void shouldReturn200WhenSellerReadsOwnProduct() throws Exception {
        // GIVEN
        Product airMax = airMaxOf(KARIM);
        when(getSellerProductUseCase.getSellerProduct(KARIM, airMax.getId())).thenReturn(airMax);

        // WHEN / THEN
        mockMvc.perform(get(PRODUCTS + "/" + airMax.getId()).header(SellerProductController.SELLER_HEADER, "seller-karim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max 90 Infrared"))
                .andExpect(jsonPath("$.variants").isArray())
                .andExpect(jsonPath("$.variants").isEmpty());
    }

    @Test
    @DisplayName("SHOULD return the variants with their price in cents WHEN the product has some")
    void shouldReturnVariantsWhenProductHasSome() throws Exception {
        // GIVEN
        Product airMax = airMaxOf(KARIM);
        Variant variant = airMax.addVariant("42", "Rouge", Money.euros(16000));
        when(getSellerProductUseCase.getSellerProduct(KARIM, airMax.getId())).thenReturn(airMax);

        // WHEN / THEN
        mockMvc.perform(get(PRODUCTS + "/" + airMax.getId()).header(SellerProductController.SELLER_HEADER, "seller-karim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.variants[0].sku").value(variant.getSku().toString()))
                .andExpect(jsonPath("$.variants[0].size").value("42"))
                .andExpect(jsonPath("$.variants[0].color").value("Rouge"))
                .andExpect(jsonPath("$.variants[0].price.amount").value(16000))
                .andExpect(jsonPath("$.variants[0].price.currency").value("EUR"));
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN the product is not found for this seller")
    void shouldReturn404WhenProductIsNotFound() throws Exception {
        // GIVEN
        ProductId unknown = ProductId.newId();
        when(getSellerProductUseCase.getSellerProduct(KARIM, unknown)).thenThrow(new ProductNotFoundException(unknown));

        // WHEN / THEN
        mockMvc.perform(get(PRODUCTS + "/" + unknown).header(SellerProductController.SELLER_HEADER, "seller-karim"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Product " + unknown + " not found"))
                .andExpect(jsonPath("$.instance").value(PRODUCTS + "/" + unknown));
    }

    @Test
    @DisplayName("SHOULD return 201 with the variant and no location WHEN the variant is valid")
    void shouldReturn201WhenVariantIsValid() throws Exception {
        // GIVEN
        ProductId productId = ProductId.newId();
        Variant variant = Variant.restore(Sku.newSku(), "42", "Rouge", Money.euros(16000));
        when(addVariantUseCase.addVariant(any(AddVariantCommand.class))).thenReturn(variant);

        // WHEN / THEN
        postVariant(productId, variant().build())
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.sku").value(variant.getSku().toString()))
                .andExpect(jsonPath("$.size").value("42"))
                .andExpect(jsonPath("$.color").value("Rouge"))
                .andExpect(jsonPath("$.price.amount").value(16000))
                .andExpect(jsonPath("$.price.currency").value("EUR"));
        ArgumentCaptor<AddVariantCommand> command = ArgumentCaptor.forClass(AddVariantCommand.class);
        verify(addVariantUseCase).addVariant(command.capture());
        assertThat(command.getValue().getSellerId()).isEqualTo(KARIM);
        assertThat(command.getValue().getProductId()).isEqualTo(productId);
        assertThat(command.getValue().getPrice()).isEqualTo(Money.euros(16000));
    }

    @Test
    @DisplayName("SHOULD return 201 WHEN each field is at its maximum length")
    void shouldReturn201WhenEachFieldIsAtItsMaximumLength() throws Exception {
        // GIVEN
        Variant variant = Variant.restore(Sku.newSku(), "42", "Rouge", Money.euros(16000));
        when(addVariantUseCase.addVariant(any(AddVariantCommand.class))).thenReturn(variant);

        // WHEN / THEN
        postVariant(ProductId.newId(), variant().size("a".repeat(20)).color("b".repeat(40)).build())
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field, without calling the use case, WHEN the variant is invalid")
    @MethodSource("invalidVariants")
    void shouldReturn400WithFieldWhenVariantIsInvalid(final AddVariantRequestDto requestDto, final String field,
                                                      final String code) throws Exception {
        // WHEN / THEN
        postVariant(ProductId.newId(), requestDto)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].code").value(code));
        verify(addVariantUseCase, never()).addVariant(any());
    }

    private static Stream<Arguments> invalidVariants() {
        return Stream.of(
                arguments(variant().size(" ").build(), "size", "NOT_BLANK"),
                arguments(variant().color(" ").build(), "color", "NOT_BLANK"),
                arguments(variant().size("a".repeat(21)).build(), "size", "SIZE"),
                arguments(variant().color("b".repeat(41)).build(), "color", "SIZE"),
                arguments(variant().price(null).build(), "price", "NOT_NULL"),
                arguments(variantPriced(null, "EUR"), "price.amount", "NOT_NULL"),
                arguments(variantPriced(0L, "EUR"), "price.amount", "POSITIVE"),
                arguments(variantPriced(-1L, "EUR"), "price.amount", "POSITIVE"),
                arguments(variantPriced(16000L, null), "price.currency", "NOT_NULL"),
                arguments(variantPriced(16000L, "USD"), "price.currency", "PATTERN"),
                arguments(variantPriced(16000L, "ABC"), "price.currency", "PATTERN"));
    }

    @Test
    @DisplayName("SHOULD return 400 MALFORMED_REQUEST, without calling the use case, WHEN the amount has decimals")
    void shouldReturn400WhenAmountHasDecimals() throws Exception {
        // GIVEN
        String decimalAmount = """
                { "size": "42", "color": "Rouge", "price": { "amount": 160.50, "currency": "EUR" } }
                """;

        // WHEN / THEN
        mockMvc.perform(post(variantsOf(ProductId.newId())).header(SellerProductController.SELLER_HEADER, "seller-karim")
                        .contentType(MediaType.APPLICATION_JSON).content(decimalAmount))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verify(addVariantUseCase, never()).addVariant(any());
    }

    @Test
    @DisplayName("SHOULD return 400 MISSING_HEADER WHEN adding a variant without the seller header")
    void shouldReturn400WhenAddingVariantWithoutSellerHeader() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(variantsOf(ProductId.newId())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variant().build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
        verify(addVariantUseCase, never()).addVariant(any());
    }

    @Test
    @DisplayName("SHOULD return 400 INVALID_PARAMETER WHEN the product id is not a UUID")
    void shouldReturn400WhenProductIdIsNotUuid() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS + "/not-a-uuid/variants").header(SellerProductController.SELLER_HEADER, "seller-karim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variant().build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        verify(addVariantUseCase, never()).addVariant(any());
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN adding a variant to a product of another seller")
    void shouldReturn404WhenAddingVariantToProductOfAnotherSeller() throws Exception {
        // GIVEN
        ProductId productId = ProductId.newId();
        when(addVariantUseCase.addVariant(any(AddVariantCommand.class)))
                .thenThrow(new ProductNotFoundException(productId));

        // WHEN / THEN
        postVariant(productId, variant().build())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("SHOULD return 409 DUPLICATE_VARIANT WHEN the product already has this size and color")
    void shouldReturn409WhenVariantAlreadyExists() throws Exception {
        // GIVEN
        Variant existing = Variant.restore(Sku.newSku(), "42", "Rouge", Money.euros(16000));
        when(addVariantUseCase.addVariant(any(AddVariantCommand.class)))
                .thenThrow(new DuplicateVariantException(existing));

        // WHEN / THEN
        postVariant(ProductId.newId(), variant().build())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VARIANT"));
    }

    @Test
    @DisplayName("SHOULD return 409 PRODUCT_MODIFIED_CONCURRENTLY WHEN the product changes during the add")
    void shouldReturn409WhenProductChangesDuringAdd() throws Exception {
        // GIVEN
        ProductId productId = ProductId.newId();
        when(addVariantUseCase.addVariant(any(AddVariantCommand.class)))
                .thenThrow(new ProductModifiedConcurrentlyException(productId));

        // WHEN / THEN
        postVariant(productId, variant().build())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_MODIFIED_CONCURRENTLY"));
    }

    private ResultActions postVariant(final ProductId productId, final AddVariantRequestDto requestDto) throws Exception {
        return mockMvc.perform(post(variantsOf(productId)).header(SellerProductController.SELLER_HEADER, "seller-karim")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDto)));
    }

    private static String variantsOf(final ProductId productId) {
        return PRODUCTS + "/" + productId + "/variants";
    }

    private static AddVariantRequestDto.AddVariantRequestDtoBuilder variant() {
        return AddVariantRequestDto.builder().size("42").color("Rouge")
                .price(MoneyRequestDto.builder().amount(16000L).currency("EUR").build());
    }

    private static AddVariantRequestDto variantPriced(final Long amount, final String currency) {
        return variant().price(MoneyRequestDto.builder().amount(amount).currency(currency).build()).build();
    }

    private static Product airMaxOf(final SellerId sellerId) {
        return Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
    }
}
