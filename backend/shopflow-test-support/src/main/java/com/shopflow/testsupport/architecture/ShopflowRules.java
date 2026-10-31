package com.shopflow.testsupport.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Conventions propres à ShopFlow (docs/03-conventions.md).
 */
public final class ShopflowRules {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";

    /** Publier directement sur Kafka ferait perdre la garantie de l'outbox : aucun événement perdu. */
    @ArchTest
    public static final ArchRule EVENTS_GO_THROUGH_THE_OUTBOX = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.kafka.core.KafkaTemplate")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.shopflow.starter.cloudevents.KafkaEventPublisher")
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule MONGODB_STAYS_IN_ITS_ADAPTER = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.mongo..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data.mongodb..")
            .allowEmptyShould(true);

    /** Les montants sont des entiers en centimes ({@code Money}). */
    @ArchTest
    public static final ArchRule NO_FLOATING_POINT_IN_THE_DOMAIN = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
            .should().haveRawType(double.class)
            .orShould().haveRawType(Double.class)
            .orShould().haveRawType(float.class)
            .orShould().haveRawType(Float.class)
            .allowEmptyShould(true);

    /** L'heure vient d'un {@code Clock} injecté (ou d'un paramètre), pour que les tests puissent la fixer. */
    @ArchTest
    public static final ArchRule TIME_COMES_FROM_A_CLOCK = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule NO_RECORDS = noClasses()
            .should().beRecords()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule REQUEST_DTOS_ARE_NAMED_REQUEST_DTO = classes()
            .that().resideInAPackage("..infrastructure.web.dto.request..").and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("RequestDto")
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule RESPONSE_DTOS_ARE_NAMED_RESPONSE_DTO = classes()
            .that().resideInAPackage("..infrastructure.web.dto.response..").and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("ResponseDto")
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule CONTROLLERS_ARE_NAMED_CONTROLLER = classes()
            .that().resideInAPackage("..infrastructure.web.controller..").and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    private ShopflowRules() {
    }
}
