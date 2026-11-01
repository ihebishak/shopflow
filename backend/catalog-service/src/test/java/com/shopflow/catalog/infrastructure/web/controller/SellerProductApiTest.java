package com.shopflow.catalog.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopflow.catalog.infrastructure.web.dto.request.AddVariantRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.ChangeVariantPriceRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.CreateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.MoneyRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.request.UpdateProductRequestDto;
import com.shopflow.catalog.infrastructure.web.dto.response.MoneyResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.ProductResponseDto;
import com.shopflow.catalog.infrastructure.web.dto.response.VariantResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'API vendeur de bout en bout : HTTP → cas d'usage → domaine → vrai MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://seller-product-api"
})
@EmbeddedKafka(partitions = 1)
@Testcontainers
class SellerProductApiTest {

    private static final String PRODUCTS = "/api/v1/seller/products";

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("SHOULD create a draft and return its location WHEN a seller creates a product")
    void shouldCreateDraftWhenSellerCreatesProduct() {
        // WHEN
        ResponseEntity<ProductResponseDto> response = createAirMax("seller-karim");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponseDto product = response.getBody();
        assertThat(product).isNotNull();
        assertThat(product.getStatus()).isEqualTo("DRAFT");
        assertThat(product.getSellerId()).isEqualTo("seller-karim");
        assertThat(product.getName()).isEqualTo("Air Max 90 Infrared");
        assertThat(product.getDescription()).isEqualTo("La réédition du modèle de 1990");
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getVariants()).isEmpty();
        assertThat(response.getHeaders().getLocation()).hasPath(PRODUCTS + "/" + product.getId());
    }

    @Test
    @DisplayName("SHOULD return the same product WHEN reading it back from its location")
    void shouldReturnSameProductWhenReadingItBack() {
        // GIVEN
        ResponseEntity<ProductResponseDto> created = createAirMax("seller-karim");

        // WHEN
        ResponseEntity<ProductResponseDto> read = restTemplate.exchange(created.getHeaders().getLocation().getPath(),
                HttpMethod.GET, new HttpEntity<>(sellerHeader("seller-karim")), ProductResponseDto.class);

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read.getBody()).isEqualTo(created.getBody());
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN another seller reads the product")
    void shouldReturn404WhenAnotherSellerReadsProduct() {
        // GIVEN
        URI location = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        ResponseEntity<JsonNode> read = getProblem(location.getPath(), "seller-lea");

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(read.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND WHEN the product does not exist")
    void shouldReturn404WhenProductDoesNotExist() {
        // WHEN
        ResponseEntity<JsonNode> read = getProblem(PRODUCTS + "/" + UUID.randomUUID(), "seller-karim");

        // THEN
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(read.getBody().get("correlationId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("SHOULD return 400 VALIDATION_FAILED naming the field WHEN the name is blank")
    void shouldReturn400WhenNameIsBlank() {
        // GIVEN
        CreateProductRequestDto requestDto = CreateProductRequestDto.builder()
                .name(" ").brand("Nike").category("sneakers").build();

        // WHEN
        ResponseEntity<JsonNode> response = restTemplate.exchange(PRODUCTS, HttpMethod.POST,
                new HttpEntity<>(requestDto, sellerHeader("seller-karim")), JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().get("errors").get(0).get("field").asText()).isEqualTo("name");
    }

    @Test
    @DisplayName("SHOULD return the variant and show it in the product WHEN a seller adds a variant")
    void shouldShowVariantInProductWhenSellerAddsVariant() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        ResponseEntity<VariantResponseDto> added = addVariant(product, "seller-karim", "42", VariantResponseDto.class);

        // THEN
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(added.getHeaders().getLocation()).isNull();
        VariantResponseDto variant = added.getBody();
        assertThat(variant).isNotNull();
        assertThat(variant.getSku()).isNotBlank();
        assertThat(variant.getSize()).isEqualTo("42");
        assertThat(variant.getColor()).isEqualTo("Rouge");
        assertThat(variant.getPrice()).isEqualTo(MoneyResponseDto.builder().amount(16000).currency("EUR").build());
        assertThat(readVariants(product)).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD keep both variants WHEN a seller adds a second variant")
    void shouldKeepBothVariantsWhenSellerAddsSecondVariant() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto first = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        VariantResponseDto second = addVariant(product, "seller-karim", "43", VariantResponseDto.class).getBody();

        // THEN
        assertThat(readVariants(product)).containsExactly(first, second);
    }

    @Test
    @DisplayName("SHOULD return 409 DUPLICATE_VARIANT and keep one variant WHEN the same variant is added twice")
    void shouldReturn409WhenSameVariantIsAddedTwice() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        addVariant(product, "seller-karim", "42", VariantResponseDto.class);

        // WHEN
        ResponseEntity<JsonNode> response = addVariant(product, "seller-karim", "42", JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code").asText()).isEqualTo("DUPLICATE_VARIANT");
        assertThat(readVariants(product)).hasSize(1);
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND and add nothing WHEN another seller adds a variant")
    void shouldReturn404WhenAnotherSellerAddsVariant() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        ResponseEntity<JsonNode> response = addVariant(product, "seller-lea", "42", JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(readVariants(product)).isEmpty();
    }

    @Test
    @DisplayName("SHOULD return and keep the new information WHEN a seller updates a product")
    void shouldKeepNewInformationWhenSellerUpdatesProduct() {
        // GIVEN
        ProductResponseDto airMax = createAirMax("seller-karim").getBody();
        URI product = URI.create(PRODUCTS + "/" + airMax.getId());

        // WHEN
        ResponseEntity<ProductResponseDto> updated = updateProduct(product, "seller-karim", ProductResponseDto.class,
                UpdateProductRequestDto.builder().name("Stan Smith").brand("Adidas").category("tennis")
                        .description("Le classique du court").build());

        // THEN
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProductResponseDto body = updated.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getName()).isEqualTo("Stan Smith");
        assertThat(body.getBrand()).isEqualTo("Adidas");
        assertThat(body.getCategory()).isEqualTo("tennis");
        assertThat(body.getDescription()).isEqualTo("Le classique du court");
        assertThat(body.getId()).isEqualTo(airMax.getId());
        assertThat(body.getSellerId()).isEqualTo(airMax.getSellerId());
        assertThat(body.getStatus()).isEqualTo(airMax.getStatus());
        assertThat(body.getCreatedAt()).isEqualTo(airMax.getCreatedAt());
        assertThat(readProduct(product)).isEqualTo(body);
    }

    @Test
    @DisplayName("SHOULD keep the variants WHEN a seller updates a product")
    void shouldKeepVariantsWhenSellerUpdatesProduct() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<ProductResponseDto> updated = updateProduct(product, "seller-karim", ProductResponseDto.class,
                airMaxUpdate().name("Air Max 90 OG").build());

        // THEN
        assertThat(updated.getBody().getVariants()).containsExactly(variant);
        assertThat(readVariants(product)).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD erase the description WHEN a seller leaves it out of the update")
    void shouldEraseDescriptionWhenSellerLeavesItOut() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        updateProduct(product, "seller-karim", ProductResponseDto.class, airMaxUpdate().description(null).build());

        // THEN
        assertThat(readProduct(product).getDescription()).isNull();
    }

    @Test
    @DisplayName("SHOULD return the product as it was WHEN a seller sends the same information")
    void shouldReturnSameProductWhenSellerSendsSameInformation() {
        // GIVEN
        ResponseEntity<ProductResponseDto> created = createAirMax("seller-karim");
        URI product = created.getHeaders().getLocation();

        // WHEN
        ResponseEntity<ProductResponseDto> updated = updateProduct(product, "seller-karim", ProductResponseDto.class,
                airMaxUpdate().build());

        // THEN
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isEqualTo(created.getBody());
        assertThat(readProduct(product)).isEqualTo(created.getBody());
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND and change nothing WHEN another seller updates the product")
    void shouldReturn404WhenAnotherSellerUpdatesProduct() {
        // GIVEN
        ResponseEntity<ProductResponseDto> created = createAirMax("seller-karim");
        URI product = created.getHeaders().getLocation();

        // WHEN
        ResponseEntity<JsonNode> response = updateProduct(product, "seller-lea", JsonNode.class,
                airMaxUpdate().name("Air Max piratée").build());

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(readProduct(product)).isEqualTo(created.getBody());
    }

    @Test
    @DisplayName("SHOULD return and keep the new price WHEN a seller changes the price of a variant")
    void shouldKeepNewPriceWhenSellerChangesPrice() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<VariantResponseDto> changed = changePrice(product, "seller-karim", variant.getSku(), 12_900L,
                VariantResponseDto.class);

        // THEN
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        VariantResponseDto body = changed.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getSku()).isEqualTo(variant.getSku());
        assertThat(body.getSize()).isEqualTo("42");
        assertThat(body.getColor()).isEqualTo("Rouge");
        assertThat(body.getPrice()).isEqualTo(MoneyResponseDto.builder().amount(12_900).currency("EUR").build());
        assertThat(readVariants(product)).containsExactly(body);
    }

    @Test
    @DisplayName("SHOULD keep the other variant and its price WHEN a seller changes the price of a variant")
    void shouldKeepOtherVariantWhenSellerChangesPrice() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto first = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();
        VariantResponseDto second = addVariant(product, "seller-karim", "43", VariantResponseDto.class).getBody();

        // WHEN
        VariantResponseDto changed = changePrice(product, "seller-karim", first.getSku(), 12_900L,
                VariantResponseDto.class).getBody();

        // THEN
        assertThat(readVariants(product)).containsExactly(changed, second);
    }

    @Test
    @DisplayName("SHOULD return the variant as it was WHEN a seller sends the same price")
    void shouldReturnSameVariantWhenSellerSendsSamePrice() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<VariantResponseDto> changed = changePrice(product, "seller-karim", variant.getSku(), 16_000L,
                VariantResponseDto.class);

        // THEN
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody()).isEqualTo(variant);
        assertThat(readVariants(product)).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND and change nothing WHEN another seller changes the price")
    void shouldReturn404WhenAnotherSellerChangesPrice() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<JsonNode> response = changePrice(product, "seller-lea", variant.getSku(), 1L, JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(readVariants(product)).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD return 404 VARIANT_NOT_FOUND and change nothing WHEN the sku belongs to another product")
    void shouldReturn404WhenSkuBelongsToAnotherProduct() {
        // GIVEN
        URI airMax = createAirMax("seller-karim").getHeaders().getLocation();
        URI otherProduct = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(airMax, "seller-karim", "42", VariantResponseDto.class).getBody();
        VariantResponseDto other = addVariant(otherProduct, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<JsonNode> response = changePrice(otherProduct, "seller-karim", variant.getSku(), 12_900L,
                JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VARIANT_NOT_FOUND");
        assertThat(readVariants(airMax)).containsExactly(variant);
        assertThat(readVariants(otherProduct)).containsExactly(other);
    }

    @Test
    @DisplayName("SHOULD keep the product published with its variants WHEN reading it back after the publication")
    void shouldKeepProductPublishedWhenReadingItBack() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        VariantResponseDto variant = addVariant(product, "seller-karim", "42", VariantResponseDto.class).getBody();

        // WHEN
        ResponseEntity<ProductResponseDto> published = publish(product, "seller-karim", ProductResponseDto.class);

        // THEN
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(published.getBody().getStatus()).isEqualTo("PUBLISHED");
        assertThat(readProduct(product)).isEqualTo(published.getBody());
        assertThat(readVariants(product)).containsExactly(variant);
    }

    @Test
    @DisplayName("SHOULD return the product as it was WHEN it is already published")
    void shouldReturnSameProductWhenAlreadyPublished() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        addVariant(product, "seller-karim", "42", VariantResponseDto.class);
        ProductResponseDto published = publish(product, "seller-karim", ProductResponseDto.class).getBody();

        // WHEN
        ResponseEntity<ProductResponseDto> again = publish(product, "seller-karim", ProductResponseDto.class);

        // THEN
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(again.getBody()).isEqualTo(published);
    }

    @Test
    @DisplayName("SHOULD stay published WHEN the seller updates the product afterwards")
    void shouldStayPublishedWhenSellerUpdatesProductAfterwards() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        addVariant(product, "seller-karim", "42", VariantResponseDto.class);
        publish(product, "seller-karim", ProductResponseDto.class);

        // WHEN
        ResponseEntity<ProductResponseDto> updated = updateProduct(product, "seller-karim", ProductResponseDto.class,
                airMaxUpdate().name("Air Max 90 OG").build());

        // THEN
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readProduct(product).getStatus()).isEqualTo("PUBLISHED");
        assertThat(readProduct(product).getName()).isEqualTo("Air Max 90 OG");
    }

    @Test
    @DisplayName("SHOULD return 409 PRODUCT_WITHOUT_VARIANT and stay a draft WHEN the product has no variant")
    void shouldReturn409WhenPublishingProductWithoutVariant() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();

        // WHEN
        ResponseEntity<JsonNode> response = publish(product, "seller-karim", JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_WITHOUT_VARIANT");
        assertThat(readProduct(product).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("SHOULD return 404 PRODUCT_NOT_FOUND and stay a draft WHEN another seller publishes the product")
    void shouldReturn404WhenAnotherSellerPublishesProduct() {
        // GIVEN
        URI product = createAirMax("seller-karim").getHeaders().getLocation();
        addVariant(product, "seller-karim", "42", VariantResponseDto.class);

        // WHEN
        ResponseEntity<JsonNode> response = publish(product, "seller-lea", JsonNode.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(readProduct(product).getStatus()).isEqualTo("DRAFT");
    }

    private <T> ResponseEntity<T> publish(final URI product, final String sellerId, final Class<T> responseType) {
        return restTemplate.exchange(product.getPath() + "/publish", HttpMethod.POST,
                new HttpEntity<>(sellerHeader(sellerId)), responseType);
    }

    private <T> ResponseEntity<T> changePrice(final URI product, final String sellerId, final String sku,
                                              final long amount, final Class<T> responseType) {
        ChangeVariantPriceRequestDto requestDto = ChangeVariantPriceRequestDto.builder()
                .price(MoneyRequestDto.builder().amount(amount).currency("EUR").build()).build();
        return restTemplate.exchange(product.getPath() + "/variants/" + sku + "/price", HttpMethod.PUT,
                new HttpEntity<>(requestDto, sellerHeader(sellerId)), responseType);
    }

    private <T> ResponseEntity<T> updateProduct(final URI product, final String sellerId, final Class<T> responseType,
                                                final UpdateProductRequestDto requestDto) {
        return restTemplate.exchange(product.getPath(), HttpMethod.PUT,
                new HttpEntity<>(requestDto, sellerHeader(sellerId)), responseType);
    }

    private static UpdateProductRequestDto.UpdateProductRequestDtoBuilder airMaxUpdate() {
        return UpdateProductRequestDto.builder().name("Air Max 90 Infrared").brand("Nike").category("sneakers")
                .description("La réédition du modèle de 1990");
    }

    private <T> ResponseEntity<T> addVariant(final URI product, final String sellerId, final String size,
                                             final Class<T> responseType) {
        AddVariantRequestDto requestDto = AddVariantRequestDto.builder().size(size).color("Rouge")
                .price(MoneyRequestDto.builder().amount(16000L).currency("EUR").build()).build();
        return restTemplate.exchange(product.getPath() + "/variants", HttpMethod.POST,
                new HttpEntity<>(requestDto, sellerHeader(sellerId)), responseType);
    }

    private List<VariantResponseDto> readVariants(final URI product) {
        return readProduct(product).getVariants();
    }

    private ProductResponseDto readProduct(final URI product) {
        return restTemplate.exchange(product.getPath(), HttpMethod.GET, new HttpEntity<>(sellerHeader("seller-karim")),
                ProductResponseDto.class).getBody();
    }

    private ResponseEntity<ProductResponseDto> createAirMax(final String sellerId) {
        CreateProductRequestDto requestDto = CreateProductRequestDto.builder()
                .name("Air Max 90 Infrared").brand("Nike").category("sneakers")
                .description("La réédition du modèle de 1990").build();
        return restTemplate.exchange(PRODUCTS, HttpMethod.POST, new HttpEntity<>(requestDto, sellerHeader(sellerId)),
                ProductResponseDto.class);
    }

    private ResponseEntity<JsonNode> getProblem(final String path, final String sellerId) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(sellerHeader(sellerId)), JsonNode.class);
    }

    private static HttpHeaders sellerHeader(final String sellerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SellerProductController.SELLER_HEADER, sellerId);
        return headers;
    }
}
