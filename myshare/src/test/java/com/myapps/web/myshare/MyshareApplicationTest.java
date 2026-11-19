package com.myapps.web.myshare;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myshare.application.port.GitHubShareClient;
import com.myapps.web.myshare.application.service.SharePageService;
import com.myapps.web.myshare.interfaces.api.ShareController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * {@link MyshareApplication}의 Spring Boot 전체 컨텍스트 로드 및 빈 등록 검증 테스트.
 *
 * <p>DB 연결이나 인증 필터 없이 애플리케이션이 정상 기동되는지 검증합니다.
 */
@SpringBootTest
class MyshareApplicationTest {

    private final ApplicationContext applicationContext;

    MyshareApplicationTest(@Autowired final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    @DisplayName("애플리케이션 컨텍스트가 DB 없이 정상 로드되고 핵심 빈이 등록된다")
    void should_loadContextAndCoreBeans_when_applicationStarts() {
        // given & when
        final ShareController controller = applicationContext.getBean(ShareController.class);
        final SharePageService service = applicationContext.getBean(SharePageService.class);
        final GitHubShareClient client = applicationContext.getBean(GitHubShareClient.class);

        // then
        assertThat(applicationContext).isNotNull();
        assertThat(controller).isNotNull();
        assertThat(service).isNotNull();
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("별도 로그인/시큐리티 필터 빈이 등록되지 않는다")
    void should_notContainSecurityBeans_when_contextLoads() {
        // given
        final String[] beanNames = applicationContext.getBeanDefinitionNames();

        // when & then
        for (final String beanName : beanNames) {
            assertThat(beanName.toLowerCase())
                    .doesNotContain("security")
                    .doesNotContain("authentication")
                    .doesNotContain("authorization");
        }
    }
}
