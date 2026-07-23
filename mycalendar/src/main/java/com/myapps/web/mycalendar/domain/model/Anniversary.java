package com.myapps.web.mycalendar.domain.model;

import java.time.LocalDate;

/**
 * 기념일 정보를 담는 값 객체.
 *
 * <p>기념일 날짜와 이름(예: "100일", "1주년")을 포함합니다.
 *
 * @param date 기념일 날짜
 * @param name 기념일 이름
 */
public record Anniversary(LocalDate date, String name) {
}
