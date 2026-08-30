package com.myapps.web.myrpg.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** GameProperties 설정을 빈 컨테이너에 등록하는 설정 클래스. */
@Configuration
@EnableConfigurationProperties(GameProperties.class)
public class GamePropertiesConfiguration {}
