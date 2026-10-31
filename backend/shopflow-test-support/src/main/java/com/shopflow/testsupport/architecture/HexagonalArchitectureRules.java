package com.shopflow.testsupport.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture hexagonale d'un service (docs/03-conventions.md § 8).
 * <p>
 * {@code allowEmptyShould} : tous les services n'ont pas toutes les couches (ex. un service sans API REST).
 * Le test de ce module vérifie que chaque règle détecte bien une violation.
 */
public final class HexagonalArchitectureRules {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String APPLICATION_API = "..application.api..";
    private static final String APPLICATION_SERVICE = "..application.service..";
    private static final String APPLICATION_SPI = "..application.spi..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String WEB = "..infrastructure.web..";
    private static final String WEB_CONTROLLER = "..infrastructure.web.controller..";
    private static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    private static final String VALIDATED = "org.springframework.validation.annotation.Validated";
    private static final String VALID = "jakarta.validation.Valid";

    @ArchTest
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .allowEmptyShould(true);

    /** Lombok n'existe qu'à la compilation (il ne laisse que son annotation {@code @Generated}). */
    @ArchTest
    public static final ArchRule DOMAIN_IS_FREE_OF_TECHNICAL_DEPENDENCIES = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..", "com.shopflow.kernel..", "lombok..")
            .allowEmptyShould(true);

    /** L'application peut utiliser Spring ({@code @Service}, {@code @Transactional}), pas les technologies. */
    @ArchTest
    public static final ArchRule APPLICATION_IS_FREE_OF_TECHNICAL_DEPENDENCIES = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data.mongodb..", "com.mongodb..", "org.bson..",
                    "org.springframework.kafka..", "org.apache.kafka..",
                    "org.springframework.web..", "jakarta.servlet..",
                    "org.apache.avro..", "com.shopflow.contracts..")
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule ADAPTERS_DO_NOT_KNOW_EACH_OTHER = slices()
            .matching("..infrastructure.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule PACKAGES_ARE_FREE_OF_CYCLES = slices()
            .matching("com.shopflow.*.(**)")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule WEB_ONLY_USES_THE_APPLICATION_API = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION_SERVICE, APPLICATION_SPI)
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule CONTROLLERS_DO_NOT_EXPOSE_THE_DOMAIN = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(WEB_CONTROLLER)
            .and().arePublic()
            .should(notExposeDomainTypes())
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule TRANSACTIONAL_METHODS_ARE_USE_CASES = noMethods()
            .that().areAnnotatedWith(TRANSACTIONAL)
            .should().beDeclaredInClassesThat().resideOutsideOfPackage(APPLICATION_SERVICE)
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule TRANSACTIONAL_CLASSES_ARE_USE_CASES = noClasses()
            .that().areAnnotatedWith(TRANSACTIONAL)
            .should().resideOutsideOfPackage(APPLICATION_SERVICE)
            .allowEmptyShould(true);

    /** Sans {@code @Validated}, le {@code @Valid} du cas d'usage est ignoré, sans aucune erreur. */
    @ArchTest
    public static final ArchRule SERVICES_RECEIVING_A_COMMAND_ARE_VALIDATED = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(APPLICATION_SERVICE)
            .and(takeACommand())
            .should().beDeclaredInClassesThat().areAnnotatedWith(VALIDATED)
            .allowEmptyShould(true);

    /** Sur l'interface : Bean Validation interdit à une implémentation d'ajouter {@code @Valid}. */
    @ArchTest
    public static final ArchRule USE_CASES_VALIDATE_THEIR_COMMANDS = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(APPLICATION_API)
            .and().areDeclaredInClassesThat().areInterfaces()
            .and(takeACommand())
            .should(validateTheirCommands())
            .allowEmptyShould(true);

    private HexagonalArchitectureRules() {
    }

    private static DescribedPredicate<JavaMethod> takeACommand() {
        return DescribedPredicate.describe("take a command",
                method -> method.getParameters().stream().anyMatch(HexagonalArchitectureRules::isCommand));
    }

    private static boolean isCommand(final JavaParameter parameter) {
        return parameter.getRawType().getSimpleName().endsWith("Command");
    }

    private static ArchCondition<JavaMethod> validateTheirCommands() {
        return new ArchCondition<>("validate their commands") {
            @Override
            public void check(final JavaMethod method, final ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    if (isCommand(parameter) && !parameter.isAnnotatedWith(VALID)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName()
                                + " does not validate " + parameter.getRawType().getSimpleName() + " with @Valid"));
                    }
                }
            }
        };
    }

    /** Regarde aussi les types génériques : {@code ResponseEntity<Product>} expose le domaine. */
    private static ArchCondition<JavaMethod> notExposeDomainTypes() {
        return new ArchCondition<>("not expose domain types") {
            @Override
            public void check(final JavaMethod method, final ConditionEvents events) {
                for (JavaClass type : method.getReturnType().getAllInvolvedRawTypes()) {
                    if (type.getPackageName().contains(".domain")) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " exposes the domain type " + type.getName()));
                    }
                }
            }
        };
    }
}
