package com.myapps.web.myrpg.application.dto;

/**
 * 게이지 UI 요소의 뷰 모델 레코드.
 *
 * <p>HP, MP, Stamina, EXP 등의 현재값/최대값과 퍼센트 채움율, 오버레이 문자열("current / max")을 담는다.
 *
 * @param current 현재값
 * @param max 최대값
 * @param percent 채움 비율 (0~100, max가 0이면 0)
 * @param overlay 수치 오버레이 문자열 ("current / max" 형식)
 */
public record GaugeView(int current, int max, int percent, String overlay) {}
