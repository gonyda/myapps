package com.myapps.web.mystudy.application.service;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 퀴즈 서비스 관련 Bean 설정.
 *
 * <p>QuizService가 의존하는 Random 객체를 Bean으로 등록합니다.
 */
@Configuration
public class QuizConfig {

    /**
     * 퀴즈 생성에 사용할 Random 인스턴스를 제공합니다.
     *
     * @return Random 인스턴스
     */
    @Bean
    public Random quizRandom() {
        return new Random();
    }
}
