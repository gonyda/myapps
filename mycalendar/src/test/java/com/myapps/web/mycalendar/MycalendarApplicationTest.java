package com.myapps.web.mycalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;
import com.myapps.web.mycalendar.interfaces.api.CalendarController;
import com.myapps.web.mycalendar.interfaces.api.ScheduleController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 애플리케이션 컨텍스트 로드 및 핵심 빈 등록을 검증하는 통합 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트가 올바르게 로드되며, 인증 메커니즘 없이
 * 모든 핵심 서비스와 컨트롤러가 정상 등록되는지 확인합니다.
 */
@SpringBootTest
class MycalendarApplicationTest {

    private final ApplicationContext applicationContext;

    MycalendarApplicationTest(@Autowired final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 애플리케이션 컨텍스트가 정상적으로 로드되는지 검증한다.
     */
    @Test
    void should_loadContext_when_applicationStarts() {
        assertThat(applicationContext).isNotNull();
    }

    /**
     * 핵심 서비스 빈들이 컨텍스트에 등록되어 있는지 검증한다.
     */
    @Test
    void should_registerServiceBeans_when_contextLoads() {
        assertThat(applicationContext.getBean(ScheduleService.class)).isNotNull();
        assertThat(applicationContext.getBean(AnniversaryCalculator.class)).isNotNull();
    }

    /**
     * 핵심 컨트롤러 빈들이 컨텍스트에 등록되어 있는지 검증한다.
     */
    @Test
    void should_registerControllerBeans_when_contextLoads() {
        assertThat(applicationContext.getBean(CalendarController.class)).isNotNull();
        assertThat(applicationContext.getBean(ScheduleController.class)).isNotNull();
    }

    /**
     * Spring Security 관련 필터가 등록되지 않음을 검증한다.
     *
     * <p>Requirement 10.2: 어떠한 사용자 인증 메커니즘도 포함하지 않음.
     */
    @Test
    void should_notContainSecurityFilters_when_contextLoads() {
        final String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (final String beanName : beanNames) {
            assertThat(beanName.toLowerCase())
                    .as("보안 관련 빈이 존재하지 않아야 합니다: %s", beanName)
                    .doesNotContain("security")
                    .doesNotContain("authentication")
                    .doesNotContain("authorization");
        }
    }
}
