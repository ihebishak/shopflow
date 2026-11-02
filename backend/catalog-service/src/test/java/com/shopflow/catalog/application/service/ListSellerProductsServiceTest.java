package com.shopflow.catalog.application.service;

import com.shopflow.catalog.application.api.query.ListSellerProductsQuery;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.ProductPage;
import com.shopflow.catalog.domain.ProductStatus;
import com.shopflow.catalog.domain.SellerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Déroulé du cas d'usage, sans Spring ni base de données.
 */
class ListSellerProductsServiceTest {

    private static final SellerId KARIM = new SellerId("seller-karim");

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ListSellerProductsService listSellerProductsService =
            new ListSellerProductsService(productRepository);

    @Test
    @DisplayName("SHOULD return the page of the repository WHEN listing with a status")
    void shouldReturnPageOfRepositoryWhenListingWithStatus() {
        // GIVEN
        ProductPage page = emptyPage();
        when(productRepository.findBySeller(KARIM, ProductStatus.PUBLISHED, 2, 50)).thenReturn(page);
        ListSellerProductsQuery query = ListSellerProductsQuery.builder().sellerId(KARIM)
                .status(ProductStatus.PUBLISHED).page(2).size(50).build();

        // WHEN
        ProductPage listed = listSellerProductsService.listSellerProducts(query);

        // THEN
        assertThat(listed).isSameAs(page);
    }

    @Test
    @DisplayName("SHOULD return the page of the repository WHEN listing without a status")
    void shouldReturnPageOfRepositoryWhenListingWithoutStatus() {
        // GIVEN
        ProductPage page = emptyPage();
        when(productRepository.findBySeller(KARIM, null, 0, 20)).thenReturn(page);
        ListSellerProductsQuery query = ListSellerProductsQuery.builder().sellerId(KARIM).page(0).size(20).build();

        // WHEN
        ProductPage listed = listSellerProductsService.listSellerProducts(query);

        // THEN
        assertThat(listed).isSameAs(page);
    }

    private static ProductPage emptyPage() {
        return ProductPage.builder().content(List.of()).page(0).size(20).totalElements(0).build();
    }
}
