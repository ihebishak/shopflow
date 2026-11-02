package com.shopflow.catalog.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Une page de produits : son contenu, sa position et le nombre total de produits trouvés.
 */
@Getter
@Builder
public final class ProductPage {

    private final List<Product> content;

    /** Numéro de la page, à partir de 0. */
    private final int page;

    private final int size;
    private final long totalElements;

    public int getTotalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
