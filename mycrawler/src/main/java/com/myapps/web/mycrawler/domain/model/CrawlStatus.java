package com.myapps.web.mycrawler.domain.model;

/** 크롤링 실행 결과의 성공/실패 상태를 나타내는 열거형. */
public enum CrawlStatus {

    /** 크롤링이 성공적으로 완료됨. */
    SUCCESS,

    /** 크롤링 중 오류가 발생하여 실패함. */
    FAILURE
}
