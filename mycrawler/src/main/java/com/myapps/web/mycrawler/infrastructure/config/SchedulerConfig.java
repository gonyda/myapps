package com.myapps.web.mycrawler.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 인프라를 활성화하는 설정 클래스.
 *
 * <p>Spring의 스케줄링 기능을 활성화하여
 * {@link com.myapps.web.mycrawler.application.service.SchedulerService}가
 * cron 기반 스케줄링을 수행할 수 있도록 합니다.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
