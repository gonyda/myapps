package com.myapps.web.myrpg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 웹 기반 솔로 RPG 애플리케이션의 메인 클래스.
 *
 * <p>캐릭터 진행상황 관리, 맵 노드 이동, 플레이 화면 서버사이드 렌더링을 제공합니다.
 */
@SpringBootApplication
public class MyrpgApplication {

    /**
     * 애플리케이션 진입점.
     *
     * @param args 커맨드라인 인수
     */
    public static void main(final String[] args) {
        SpringApplication.run(MyrpgApplication.class, args);
    }
}
