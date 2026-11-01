package com.shopflow.catalog.infrastructure.web.swagger;

import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * Documentation OpenAPI de l'API produits côté vendeur (page Swagger UI).
 * Portée par une interface pour que le contrôleur reste sans annotations de documentation.
 */
@Tag(name = "Produits (vendeur)", description = "Un vendeur crée et consulte ses produits, et leur ajoute des variantes.")
public interface SellerProductControllerDocumentation {

    String SELLER_HEADER_DESCRIPTION =
            "Identifiant du vendeur. Provisoire : remplacé par le token Keycloak en phase 2.";

    @Operation(summary = "Créer un produit",
            description = "Le produit naît en brouillon (DRAFT), invisible des clients jusqu'à sa publication.")
    @ApiResponse(responseCode = "201", description = "Produit créé ; son adresse est dans l'en-tête Location.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : VALIDATION_FAILED, INVALID_SELLER_ID, MISSING_HEADER, "
                    + "MALFORMED_REQUEST.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductResponseDto> createProduct(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            CreateProductRequestDto requestDto);

    @Operation(summary = "Consulter un de ses produits")
    @ApiResponse(responseCode = "200", description = "Le produit.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : INVALID_PARAMETER, INVALID_SELLER_ID, MISSING_HEADER.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "Produit inexistant ou appartenant à un autre vendeur. Code : PRODUCT_NOT_FOUND.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ProductResponseDto getProduct(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            @Parameter(description = "Identifiant du produit (UUID).") UUID productId);

    @Operation(summary = "Ajouter une variante à un de ses produits",
            description = "Le SKU est généré par le serveur. Le prix est un nombre entier de centimes, en euros.")
    @ApiResponse(responseCode = "201", description = "La variante créée, avec son SKU.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : VALIDATION_FAILED, INVALID_PARAMETER, INVALID_SELLER_ID, "
                    + "MISSING_HEADER, MALFORMED_REQUEST (dont un montant à virgule).",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "Produit inexistant ou appartenant à un autre vendeur. Code : PRODUCT_NOT_FOUND.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Cette taille et ce coloris existent déjà (DUPLICATE_VARIANT), ou le produit a changé "
                    + "pendant l'ajout (PRODUCT_MODIFIED_CONCURRENTLY) : il suffit alors de réessayer.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    VariantResponseDto addVariant(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            @Parameter(description = "Identifiant du produit (UUID).") UUID productId,
            AddVariantRequestDto requestDto);
}
