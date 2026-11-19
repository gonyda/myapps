package com.myapps.web.myshare.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** myshare 모듈의 계층형 아키텍처 가드레일 검증 테스트. */
@AnalyzeClasses(
        packages = "com.myapps.web.myshare",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule interfaces_should_not_be_accessed_by_application_or_domain =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.myapps.web.myshare.application..",
                            "com.myapps.web.myshare.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.myapps.web.myshare.interfaces..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("com.myapps.web.myshare.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.myapps.web.myshare.infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers =
            noClasses()
                    .that()
                    .resideInAPackage("com.myapps.web.myshare.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.myapps.web.myshare.application..",
                            "com.myapps.web.myshare.infrastructure..",
                            "com.myapps.web.myshare.interfaces..");

    @ArchTest
    static final ArchRule controllers_should_only_reside_in_interfaces =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage("com.myapps.web.myshare.interfaces.api..");
}
