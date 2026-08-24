package com.myapps.web.myrpg.infrastructure.config;

import com.myapps.web.myrpg.application.service.AuthService;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정 클래스.
 *
 * <p>{@link AuthInterceptor}를 전역 인터셉터로 등록하고, 정적 자원 및 인증 관련 경로를 화이트리스트로 제외합니다.
 */
@Configuration
@ConditionalOnBean(AuthService.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * WebMvcConfig를 생성한다.
     *
     * @param authInterceptor 인증 검사 인터셉터
     */
    public WebMvcConfig(final AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/login/**",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/h2-console/**",
                        "/actuator/**",
                        "/error");
    }
}
