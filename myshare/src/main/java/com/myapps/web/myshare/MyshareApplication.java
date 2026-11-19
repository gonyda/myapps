package com.myapps.web.myshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

/**
 * Mike가 생성한 단일 HTML 공유 페이지를 서빙하는 웹 애플리케이션 진입점.
 *
 * <p>외부 로그인이나 데이터베이스 연결 없이 공유 링크({@code /share/{slug}}) 열람을 지원합니다.
 */
@SpringBootApplication(
        exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceInitializationAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
        })
public class MyshareApplication {

    /**
     * 애플리케이션을 시작합니다.
     *
     * @param args 커맨드라인 인수
     */
    public static void main(final String[] args) {
        SpringApplication.run(MyshareApplication.class, args);
    }
}
