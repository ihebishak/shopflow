package com.shopflow.catalog.infrastructure.web;

import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.FindSellerProductUseCase;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * API des produits côté vendeur.
 * <p>
 * Le vendeur est identifié par l'en-tête {@code X-Seller-Id}, en attendant Keycloak (phase 2).
 */
@RestController
@RequestMapping("/api/v1/seller/products")
class SellerProductController {

    static final String SELLER_HEADER = "X-Seller-Id";

    private final CreateProductUseCase createProductUseCase;
    private final FindSellerProductUseCase findSellerProductUseCase;

    SellerProductController(CreateProductUseCase createProductUseCase,
                            FindSellerProductUseCase findSellerProductUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.findSellerProductUseCase = findSellerProductUseCase;
    }

    /** Crée un brouillon : 201 Created, avec l'adresse du produit dans l'en-tête {@code Location}. */
    @PostMapping
    ResponseEntity<ProductResponse> create(@RequestHeader(SELLER_HEADER) String sellerId,
                                           @RequestBody CreateProductRequest request) {
        Product product = createProductUseCase.createProduct(request.toCommand(new SellerId(sellerId)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}").buildAndExpand(product.getId()).toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(product));
    }

    /** Un des produits du vendeur : 200, ou 404 s'il n'existe pas ou appartient à un autre vendeur. */
    @GetMapping("/{productId}")
    ResponseEntity<ProductResponse> get(@RequestHeader(SELLER_HEADER) String sellerId,
                                        @PathVariable UUID productId) {
        return findSellerProductUseCase.findSellerProduct(new SellerId(sellerId), new ProductId(productId))
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
