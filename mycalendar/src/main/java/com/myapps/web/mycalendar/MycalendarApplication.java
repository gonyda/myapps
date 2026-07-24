package com.myapps.web.mycalendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 커플 일정 관리 웹 애플리케이션의 메인 클래스.
 *
 * <p>인증 없이 접근 가능한 모바일 우선 캘린더 서비스를 제공합니다.
 */
@SpringBootApplication
public class MycalendarApplication {

    /**
     * 애플리케이션 진입점.
     *
     * @param args 커맨드라인 인수
     */
    public static void main(final String[] args) {
        String tnsAdmin = System.getenv("TNS_ADMIN");
        if (tnsAdmin != null && !tnsAdmin.isEmpty()) {
            System.setProperty("oracle.net.tns_admin", tnsAdmin);
        }
        SpringApplication.run(MycalendarApplication.class, args);
    }
}
