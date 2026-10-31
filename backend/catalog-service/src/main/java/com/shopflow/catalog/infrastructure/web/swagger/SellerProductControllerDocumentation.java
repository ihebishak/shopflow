package com.shopflow.catalog.infrastructure.web.swagger;

import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * Documentation OpenAPI de l'API produits côté vendeur (page Swagger UI).
 * Portée par une interface pour que le contrôleur reste sans annotations de documentation.
 */
@Tag(name = "Produits (vendeur)", description = "Un vendeur crée et consulte ses produits.")
public interface SellerProductControllerDocumentation {

    String SELLER_HEADER_DESCRIPTION =
            "Identifiant du vendeur. Provisoire : remplacé par le token Keycloak en phase 2.";

    @Operation(summary = "Créer un produit",
            description = "Le produit naît en brouillon (DRAFT), invisible des clients jusqu'à sa publication.")
    @ApiResponse(responseCode = "201", description = "Produit créé ; son adresse est dans l'en-tête Location.")
    @ApiResponse(responseCode = "400", description = "Champ invalide ou en-tête X-Seller-Id absent.",
            content = @Content)
    ResponseEntity<ProductResponseDto> createProduct(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            CreateProductRequestDto requestDto);

    @Operation(summary = "Consulter un de ses produits")
    @ApiResponse(responseCode = "200", description = "Le produit.")
    @ApiResponse(responseCode = "404", description = "Produit inexistant ou appartenant à un autre vendeur.",
            content = @Content)
    ResponseEntity<ProductResponseDto> getProduct(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            @Parameter(description = "Identifiant du produit (UUID).") UUID productId);
}
