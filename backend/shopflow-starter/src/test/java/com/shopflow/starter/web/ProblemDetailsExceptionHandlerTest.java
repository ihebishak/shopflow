package com.shopflow.starter.web;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chaque sorte d'erreur donne une réponse Problem Details complète et de même forme.
 * Le gestionnaire est branché par l'auto-configuration du starter, comme dans un vrai service.
 */
@WebMvcTest
class ProblemDetailsExceptionHandlerTest {

    private static final String CORRELATION_ID = "corr-test-42";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SHOULD answer 404 with the business code WHEN a NOT_FOUND business exception is thrown")
    void shouldAnswer404WhenNotFoundBusinessExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/test/things/42").header("X-Correlation-Id", CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://shopflow.dev/errors/thing-not-found"))
                .andExpect(jsonPath("$.title").value("Thing not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Thing 42 not found"))
                .andExpect(jsonPath("$.instance").value("/test/things/42"))
                .andExpect(jsonPath("$.code").value("THING_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    @DisplayName("SHOULD answer 409 WHEN a CONFLICT business exception is thrown")
    void shouldAnswer409WhenConflictBusinessExceptionIsThrown() throws Exception {
        mockMvc.perform(post("/test/things/42/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("THING_ALREADY_PUBLISHED"));
    }

    @Test
    @DisplayName("SHOULD answer 400 with the field in errors WHEN an INVALID business exception names a field")
    void shouldAnswer400WithFieldWhenInvalidBusinessExceptionNamesField() throws Exception {
        mockMvc.perform(post("/test/things/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_THING"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_THING"))
                .andExpect(jsonPath("$.errors[0].message").value("name is required"));
    }

    @Test
    @DisplayName("SHOULD answer 400 VALIDATION_FAILED listing each invalid field WHEN bean validation fails")
    void shouldAnswer400ListingInvalidFieldsWhenBeanValidationFails() throws Exception {
        mockMvc.perform(post("/test/things").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \" \" }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("The request contains 1 invalid field(s)."))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.errors[0].message").value("name is required"));
    }

    @Test
    @DisplayName("SHOULD answer 400 MISSING_HEADER WHEN a required header is absent")
    void shouldAnswer400WhenRequiredHeaderIsAbsent() throws Exception {
        mockMvc.perform(get("/test/whoami"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"))
                .andExpect(jsonPath("$.detail").value("Required header 'X-Seller-Id' is missing."));
    }

    @Test
    @DisplayName("SHOULD answer 400 INVALID_PARAMETER WHEN a path parameter is not a valid UUID")
    void shouldAnswer400WhenPathParameterIsNotUuid() throws Exception {
        mockMvc.perform(get("/test/uuids/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid value: 'not-a-uuid'."));
    }

    @Test
    @DisplayName("SHOULD answer 400 MALFORMED_REQUEST WHEN the body is not valid JSON")
    void shouldAnswer400WhenBodyIsNotValidJson() throws Exception {
        mockMvc.perform(post("/test/things").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("SHOULD answer 500 INTERNAL_ERROR without technical details WHEN an unexpected error occurs")
    void shouldAnswer500WithoutDetailsWhenUnexpectedErrorOccurs() throws Exception {
        mockMvc.perform(get("/test/boom").header("X-Correlation-Id", CORRELATION_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
                .andExpect(content().string(not(containsString("database password"))));
    }

    @Test
    @DisplayName("SHOULD complete Spring's own technical errors with a code WHEN the HTTP method is not supported")
    void shouldCompleteSpringTechnicalErrorsWhenMethodIsNotSupported() throws Exception {
        mockMvc.perform(put("/test/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.type").value("https://shopflow.dev/errors/method-not-allowed"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @SpringBootConfiguration
    static class TestApplication {

        @Bean
        ThingController thingController() {
            return new ThingController();
        }
    }

    /** Contrôleur de test : chaque adresse provoque une sorte d'erreur. */
    @RestController
    static class ThingController {

        @GetMapping("/test/things/{id}")
        String find(@PathVariable final String id) {
            throw new ThingNotFoundException(id);
        }

        @PostMapping("/test/things/{id}/publish")
        String publish(@PathVariable final String id) {
            throw new ThingAlreadyPublishedException();
        }

        @PostMapping("/test/things/invalid")
        String invalid() {
            throw new InvalidThingException();
        }

        @PostMapping("/test/things")
        String create(@Valid @RequestBody final ThingRequestDto requestDto) {
            return requestDto.getName();
        }

        @GetMapping("/test/whoami")
        String whoAmI(@RequestHeader("X-Seller-Id") final String sellerId) {
            return sellerId;
        }

        @GetMapping("/test/uuids/{id}")
        String uuid(@PathVariable final UUID id) {
            return id.toString();
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("database password is hunter2");
        }
    }

    @Getter
    @Builder
    @Jacksonized
    static class ThingRequestDto {

        @NotBlank(message = "name is required")
        @Size(max = 10, message = "name must be at most {max} characters")
        private final String name;
    }

    static class ThingNotFoundException extends BusinessException {
        ThingNotFoundException(final String id) {
            super(ErrorKind.NOT_FOUND, "THING_NOT_FOUND", "Thing " + id + " not found");
        }
    }

    static class ThingAlreadyPublishedException extends BusinessException {
        ThingAlreadyPublishedException() {
            super(ErrorKind.CONFLICT, "THING_ALREADY_PUBLISHED", "Thing is already published");
        }
    }

    static class InvalidThingException extends BusinessException {
        InvalidThingException() {
            super(ErrorKind.INVALID, "INVALID_THING", "name", "name is required");
        }
    }
}
