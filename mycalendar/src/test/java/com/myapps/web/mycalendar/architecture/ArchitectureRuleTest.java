package com.myapps.web.mycalendar.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** mycalendar 모듈의 아키텍처 가드레일 검증 테스트. */
@AnalyzeClasses(
        packages = "com.myapps.web.mycalendar",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule interfaces_should_not_be_accessed_by_application_or_domain =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.myapps.web.mycalendar.application..",
                            "com.myapps.web.mycalendar.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.myapps.web.mycalendar.interfaces..");

    @ArchTest
    static final ArchRule controllers_should_only_reside_in_interfaces =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage("com.myapps.web.mycalendar.interfaces.api..");
}
