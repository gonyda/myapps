package com.myapps.web.mycrawler.domain.model;

/** 크롤링 실행의 트리거 출처를 나타내는 열거형. */
public enum TriggerSource {

    /** 스케줄러에 의해 자동으로 실행됨. */
    SCHEDULED,

    /** 사용자가 Admin UI에서 수동으로 실행함. */
    MANUAL
}
