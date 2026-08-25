package com.myapps.web.myrpg.domain.model;

/**
 * 스킬의 랭크별 효과 항목을 표현하는 불변 Record.
 *
 * @param label 효과 항목 라벨 (예: "1히트당 피해", "피해 경감률", "생명력 회복량")
 * @param currentValue 현재 랭크 수치 문자열 (예: "90%", "70%", "30 HP")
 * @param nextValue 다음 랭크 수치 문자열 (MASTER면 null)
 */
public record SkillEffectRowView(String label, String currentValue, String nextValue) {}
