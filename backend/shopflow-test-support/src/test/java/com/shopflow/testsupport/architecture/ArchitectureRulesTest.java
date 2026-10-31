package com.shopflow.testsupport.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Teste les règles elles-mêmes, sur deux mini-applications d'exemple : une règle mal écrite
 * (ex. un nom de package erroné) ne détecterait jamais rien, sans que personne le voie.
 */
class ArchitectureRulesTest {

    private static final String FIXTURE = "com.shopflow.testsupport.architecture.fixture";
    private static final JavaClasses GOOD_APPLICATION = new ClassFileImporter().importPackages(FIXTURE + ".good");
    private static final JavaClasses BAD_APPLICATION = new ClassFileImporter().importPackages(FIXTURE + ".bad");

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    @DisplayName("SHOULD report nothing WHEN the application respects the rule")
    void shouldReportNothingWhenApplicationRespectsRule(final String name, final ArchRule rule, final String expected) {
        // WHEN
        EvaluationResult result = rule.evaluate(GOOD_APPLICATION);

        // THEN
        assertThat(result.hasViolation()).as(result.getFailureReport().toString()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    @DisplayName("SHOULD report the violation WHEN the application breaks the rule")
    void shouldReportViolationWhenApplicationBreaksRule(final String name, final ArchRule rule, final String expected) {
        // WHEN
        EvaluationResult result = rule.evaluate(BAD_APPLICATION);

        // THEN
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().toString()).contains(expected);
    }

    private static Stream<Arguments> rules() {
        return Stream.of(
                rule("domain → outer layers", HexagonalArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS, "BadThing"),
                rule("application → infrastructure", HexagonalArchitectureRules.APPLICATION_DOES_NOT_DEPEND_ON_INFRASTRUCTURE, "BadMongoRepository"),
                rule("pure domain", HexagonalArchitectureRules.DOMAIN_IS_FREE_OF_TECHNICAL_DEPENDENCIES, "org.springframework"),
                rule("technology-free application", HexagonalArchitectureRules.APPLICATION_IS_FREE_OF_TECHNICAL_DEPENDENCIES, "MongoTemplate"),
                rule("independent adapters", HexagonalArchitectureRules.ADAPTERS_DO_NOT_KNOW_EACH_OTHER, "ThingEndpoint"),
                rule("no cycles", HexagonalArchitectureRules.PACKAGES_ARE_FREE_OF_CYCLES, "Cycle"),
                rule("web through the api only", HexagonalArchitectureRules.WEB_ONLY_USES_THE_APPLICATION_API, "BadThingService"),
                rule("controller returning the domain", HexagonalArchitectureRules.CONTROLLERS_DO_NOT_EXPOSE_THE_DOMAIN, "direct()"),
                rule("controller wrapping the domain", HexagonalArchitectureRules.CONTROLLERS_DO_NOT_EXPOSE_THE_DOMAIN, "wrapped()"),
                rule("@Transactional methods", HexagonalArchitectureRules.TRANSACTIONAL_METHODS_ARE_USE_CASES, "ThingEndpoint"),
                rule("@Transactional classes", HexagonalArchitectureRules.TRANSACTIONAL_CLASSES_ARE_USE_CASES, "BadMongoRepository"),
                rule("service without @Validated", HexagonalArchitectureRules.SERVICES_RECEIVING_A_COMMAND_ARE_VALIDATED, "BadThingService"),
                rule("use case without @Valid", HexagonalArchitectureRules.USE_CASES_VALIDATE_THEIR_COMMANDS, "does not validate RenameBadThingCommand"),
                rule("outbox", ShopflowRules.EVENTS_GO_THROUGH_THE_OUTBOX, "KafkaTemplate"),
                rule("mongodb adapter", ShopflowRules.MONGODB_STAYS_IN_ITS_ADAPTER, "BadThingService"),
                rule("no floating point", ShopflowRules.NO_FLOATING_POINT_IN_THE_DOMAIN, "price"),
                rule("clock", ShopflowRules.TIME_COMES_FROM_A_CLOCK, "Instant.now()"),
                rule("no records", ShopflowRules.NO_RECORDS, "BadRecord"),
                rule("request dto naming", ShopflowRules.REQUEST_DTOS_ARE_NAMED_REQUEST_DTO, "BadRequest"),
                rule("response dto naming", ShopflowRules.RESPONSE_DTOS_ARE_NAMED_RESPONSE_DTO, "BadResponse"),
                rule("controller naming", ShopflowRules.CONTROLLERS_ARE_NAMED_CONTROLLER, "ThingEndpoint"),
                rule("field injection", CodingRules.NO_FIELD_INJECTION, "injected"),
                rule("standard streams", CodingRules.NO_STANDARD_STREAMS, "System.out"),
                rule("generic exceptions", CodingRules.NO_GENERIC_EXCEPTIONS, "RuntimeException"),
                rule("java.util.logging", CodingRules.NO_JAVA_UTIL_LOGGING, "java.util.logging"));
    }

    private static Arguments rule(final String name, final ArchRule rule, final String expected) {
        return arguments(name, rule, expected);
    }
}
