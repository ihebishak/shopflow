package com.shopflow.testsupport.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * Hygiène générale, avec les règles fournies par ArchUnit.
 */
public final class CodingRules {

    /** Injection par constructeur uniquement. */
    @ArchTest
    public static final ArchRule NO_FIELD_INJECTION =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);

    /** Tout passe par les logs, avec le {@code correlationId}. */
    @ArchTest
    public static final ArchRule NO_STANDARD_STREAMS =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true);

    @ArchTest
    public static final ArchRule NO_GENERIC_EXCEPTIONS =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.allowEmptyShould(true);

    @ArchTest
    public static final ArchRule NO_JAVA_UTIL_LOGGING =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.allowEmptyShould(true);

    private CodingRules() {
    }
}
