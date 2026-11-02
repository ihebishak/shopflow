package com.shopflow.catalog.infrastructure.web.swagger;

import com.shopflow.catalog.infrastructure.web.dto.request.CreateCategoryRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.CategoryResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Documentation OpenAPI de l'API catégories côté admin (page Swagger UI).
 * Portée par une interface pour que le contrôleur reste sans annotations de documentation.
 */
@Tag(name = "Catégories (admin)", description = "L'admin construit l'arbre des catégories du catalogue.")
public interface AdminCategoryControllerDocumentation {

    @Operation(summary = "Créer une catégorie",
            description = "Au premier niveau, ou sous une catégorie parente. L'arbre compte 3 niveaux au maximum.")
    @ApiResponse(responseCode = "201", description = "Catégorie créée.")
    @ApiResponse(responseCode = "400",
            description = "Requête invalide. Codes : VALIDATION_FAILED, INVALID_CATEGORY_SLUG, MALFORMED_REQUEST.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "La catégorie parente n'existe pas. Code : CATEGORY_NOT_FOUND.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "L'identifiant est déjà pris, ou le parent est au dernier niveau de l'arbre. "
                    + "Codes : CATEGORY_ALREADY_EXISTS, CATEGORY_TOO_DEEP.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    CategoryResponseDto createCategory(CreateCategoryRequestDto requestDto);
}
