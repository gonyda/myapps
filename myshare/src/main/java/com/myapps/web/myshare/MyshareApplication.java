package com.myapps.web.myshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mike 공유 HTML 페이지({@code /share/{slug}}) 및 미니앱 공용 JSON 저장소({@code /api/mini/{appKey}})를 제공하는 웹
 * 애플리케이션 진입점.
 */
@SpringBootApplication
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
