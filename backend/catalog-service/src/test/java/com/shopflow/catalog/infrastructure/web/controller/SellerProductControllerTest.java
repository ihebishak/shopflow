package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.FindSellerProductUseCase;
import com.shopflow.catalog.application.api.command.CreateProductCommand;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.infrastructure.web.mapper.ProductRestMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrôleur seul (couche HTTP) : les cas d'usage sont simulés, ni MongoDB ni Kafka.
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

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private FindSellerProductUseCase findSellerProductUseCase;

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
    @DisplayName("SHOULD return 400 without calling the use case WHEN the name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // GIVEN
        String blankName = """
                { "name": " ", "brand": "Nike", "category": "sneakers" }
                """;

        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).header(SellerProductController.SELLER_HEADER, "seller-karim")
                        .contentType(MediaType.APPLICATION_JSON).content(blankName))
                .andExpect(status().isBadRequest());
        verify(createProductUseCase, never()).createProduct(any());
    }

    @Test
    @DisplayName("SHOULD return 400 WHEN the seller header is missing")
    void shouldReturn400WhenSellerHeaderIsMissing() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(AIR_MAX_JSON))
                .andExpect(status().isBadRequest());
        verify(createProductUseCase, never()).createProduct(any());
    }

    @Test
    @DisplayName("SHOULD return 200 with the product WHEN the seller reads one of its products")
    void shouldReturn200WhenSellerReadsOwnProduct() throws Exception {
        // GIVEN
        Product airMax = airMaxOf(KARIM);
        when(findSellerProductUseCase.findSellerProduct(KARIM, airMax.getId())).thenReturn(Optional.of(airMax));

        // WHEN / THEN
        mockMvc.perform(get(PRODUCTS + "/" + airMax.getId()).header(SellerProductController.SELLER_HEADER, "seller-karim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max 90 Infrared"));
    }

    @Test
    @DisplayName("SHOULD return 404 WHEN the product is not found for this seller")
    void shouldReturn404WhenProductIsNotFound() throws Exception {
        // GIVEN
        ProductId unknown = ProductId.newId();
        when(findSellerProductUseCase.findSellerProduct(eq(KARIM), any(ProductId.class))).thenReturn(Optional.empty());

        // WHEN / THEN
        mockMvc.perform(get(PRODUCTS + "/" + unknown).header(SellerProductController.SELLER_HEADER, "seller-karim"))
                .andExpect(status().isNotFound());
    }

    private static Product airMaxOf(final SellerId sellerId) {
        return Product.draft(sellerId, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now());
    }
}
