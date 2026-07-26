package com.myapps.web.myrpg.domain.model;

/**
 * 스테이지 이벤트 종류를 정의하는 열거형.
 *
 * <p>던전 탐색 중 스테이지별로 발생하는 이벤트 유형이다.
 */
public enum StageEventType {

    BATTLE,
    REST,
    MERCHANT,
    TRAP,
    TREASURE
}
