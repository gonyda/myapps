package com.myapps.web.mycrawler.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** mycrawler 모듈의 아키텍처 가드레일 검증 테스트. */
@AnalyzeClasses(
        packages = "com.myapps.web.mycrawler",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule interfaces_should_not_be_accessed_by_other_layers =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.myapps.web.mycrawler.application..",
                            "com.myapps.web.mycrawler.domain..",
                            "com.myapps.web.mycrawler.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.myapps.web.mycrawler.interfaces..");

    @ArchTest
    static final ArchRule controllers_should_only_reside_in_interfaces =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage("com.myapps.web.mycrawler.interfaces.api..");
}
