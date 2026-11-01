package com.shopflow.catalog.infrastructure.web.swagger;

import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.ChangeVariantPriceRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.UpdateProductRequestDto;
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
@Tag(name = "Produits (vendeur)", description = "Un vendeur crée, consulte et modifie ses produits, leur ajoute des variantes et change "
        + "leurs prix.")
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

    @Operation(summary = "Modifier les informations d'un de ses produits",
            description = "Remplace le nom, la marque, la catégorie et la description : une description absente "
                    + "efface celle qui existait. Le statut et les variantes ne changent pas.")
    @ApiResponse(responseCode = "200", description = "Le produit à jour, ou inchangé si rien n'était différent.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : VALIDATION_FAILED, INVALID_PARAMETER, INVALID_SELLER_ID, "
                    + "MISSING_HEADER, MALFORMED_REQUEST.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "Produit inexistant ou appartenant à un autre vendeur. Code : PRODUCT_NOT_FOUND.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Le produit a changé pendant la modification : le relire avant de réessayer. "
                    + "Code : PRODUCT_MODIFIED_CONCURRENTLY.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ProductResponseDto updateProduct(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            @Parameter(description = "Identifiant du produit (UUID).") UUID productId,
            UpdateProductRequestDto requestDto);

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

    @Operation(summary = "Changer le prix d'une variante d'un de ses produits",
            description = "Le prix est un nombre entier de centimes, en euros. Les autres services sont prévenus "
                    + "de l'ancien et du nouveau prix.")
    @ApiResponse(responseCode = "200", description = "La variante à son nouveau prix, ou inchangée si c'était le même.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : VALIDATION_FAILED, INVALID_PARAMETER, INVALID_SELLER_ID, "
                    + "MISSING_HEADER, MALFORMED_REQUEST (dont un montant à virgule).",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404",
            description = "Produit inexistant ou appartenant à un autre vendeur (PRODUCT_NOT_FOUND), ou SKU absent "
                    + "de ce produit (VARIANT_NOT_FOUND).",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Le produit a changé pendant la modification : le relire avant de réessayer. "
                    + "Code : PRODUCT_MODIFIED_CONCURRENTLY.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    VariantResponseDto changeVariantPrice(
            @Parameter(description = SELLER_HEADER_DESCRIPTION, example = "seller-karim") String sellerId,
            @Parameter(description = "Identifiant du produit (UUID).") UUID productId,
            @Parameter(description = "SKU de la variante (UUID).") UUID sku,
            ChangeVariantPriceRequestDto requestDto);
}
