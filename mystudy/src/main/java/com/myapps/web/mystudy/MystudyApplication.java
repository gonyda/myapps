package com.myapps.web.mystudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mystudy 모듈의 Spring Boot 애플리케이션 진입점.
 *
 * <p>Spring Boot 자동 설정 및 컴포넌트 스캔을 활성화하고 애플리케이션을 시작합니다.
 */
@SpringBootApplication
public class MystudyApplication {

    /**
     * 애플리케이션을 시작합니다.
     *
     * @param args 커맨드라인 인수
     */
    public static void main(final String[] args) {
        SpringApplication.run(MystudyApplication.class, args);
    }
}
