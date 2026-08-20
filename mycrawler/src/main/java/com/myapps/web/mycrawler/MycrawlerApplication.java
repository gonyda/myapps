package com.myapps.web.mycrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * mycrawler 모듈의 Spring Boot 애플리케이션 엔트리 포인트.
 *
 * <p>Playwright Java 기반 웹 크롤링 엔진을 제공하는 웹 애플리케이션입니다.
 */
@SpringBootApplication(
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ConfigurationPropertiesScan
public class MycrawlerApplication {

    /**
     * 애플리케이션을 시작합니다.
     *
     * @param args 커맨드라인 인수
     */
    public static void main(final String[] args) {
        SpringApplication.run(MycrawlerApplication.class, args);
    }
}
