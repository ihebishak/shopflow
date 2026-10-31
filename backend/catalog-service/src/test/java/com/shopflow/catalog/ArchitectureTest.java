package com.shopflow.catalog;

import com.shopflow.testsupport.architecture.CodingRules;
import com.shopflow.testsupport.architecture.HexagonalArchitectureRules;
import com.shopflow.testsupport.architecture.ShopflowRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/**
 * Applique au catalogue les règles d'architecture partagées : une violation fait échouer le build.
 */
@AnalyzeClasses(packages = "com.shopflow.catalog", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests HEXAGONAL_ARCHITECTURE = ArchTests.in(HexagonalArchitectureRules.class);

    @ArchTest
    static final ArchTests SHOPFLOW_CONVENTIONS = ArchTests.in(ShopflowRules.class);

    @ArchTest
    static final ArchTests CODING_RULES = ArchTests.in(CodingRules.class);
}
