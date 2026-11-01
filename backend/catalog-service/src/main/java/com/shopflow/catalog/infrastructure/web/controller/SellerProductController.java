package com.shopflow.catalog.infrastructure.web.controller;

import com.shopflow.catalog.application.api.AddVariantUseCase;
import com.shopflow.catalog.application.api.CreateProductUseCase;
import com.shopflow.catalog.application.api.GetSellerProductUseCase;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductId;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import com.shopflow.catalog.infrastructure.web.mapper.ProductRestMapper;
import com.shopflow.catalog.infrastructure.web.swagger.SellerProductControllerDocumentation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * API des produits côté vendeur : convertit (mapper) et délègue aux cas d'usage, sans logique métier.
 * Les erreurs sont transformées en Problem Details par le gestionnaire du starter.
 * <p>
 * Le vendeur est identifié par l'en-tête {@code X-Seller-Id}, en attendant Keycloak (phase 2).
 */
@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
public class SellerProductController implements SellerProductControllerDocumentation {

    public static final String SELLER_HEADER = "X-Seller-Id";

    private final CreateProductUseCase createProductUseCase;
    private final GetSellerProductUseCase getSellerProductUseCase;
    private final AddVariantUseCase addVariantUseCase;
    private final ProductRestMapper productRestMapper;

    /** Crée un brouillon : 201 Created, avec l'adresse du produit dans l'en-tête {@code Location}. */
    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponseDto> createProduct(@RequestHeader(SELLER_HEADER) final String sellerId,
                                                            @Valid @RequestBody final CreateProductRequestDto requestDto) {
        Product product = createProductUseCase.createProduct(
                productRestMapper.toCommand(requestDto, new SellerId(sellerId)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}").buildAndExpand(product.getId()).toUri();
        return ResponseEntity.created(location).body(productRestMapper.toResponseDto(product));
    }

    /** Un des produits du vendeur. */
    @Override
    @GetMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDto getProduct(@RequestHeader(SELLER_HEADER) final String sellerId,
                                         @PathVariable final UUID productId) {
        Product product = getSellerProductUseCase.getSellerProduct(new SellerId(sellerId), new ProductId(productId));
        return productRestMapper.toResponseDto(product);
    }

    /** Ajoute une variante : 201 Created, sans en-tête {@code Location} car une variante n'a pas d'adresse propre. */
    @Override
    @PostMapping(value = "/{productId}/variants", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponseDto addVariant(@RequestHeader(SELLER_HEADER) final String sellerId,
                                         @PathVariable final UUID productId,
                                         @Valid @RequestBody final AddVariantRequestDto requestDto) {
        Variant variant = addVariantUseCase.addVariant(
                productRestMapper.toCommand(requestDto, new SellerId(sellerId), new ProductId(productId)));
        return productRestMapper.toResponseDto(variant);
    }
}
