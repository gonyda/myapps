package com.myapps.web.myrpg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * myrpg 모듈의 Spring Boot 애플리케이션 진입점.
 *
 * <p>텍스트 기반 턴제 모바일 웹 RPG의 메인 애플리케이션 클래스이다.
 */
@SpringBootApplication
public class MyrpgApplication {

    /**
     * 애플리케이션을 시작한다.
     *
     * @param args 커맨드라인 인자
     */
    public static void main(final String[] args) {
        SpringApplication.run(MyrpgApplication.class, args);
    }
}
