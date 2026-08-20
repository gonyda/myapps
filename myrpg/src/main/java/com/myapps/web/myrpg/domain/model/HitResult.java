package com.myapps.web.myrpg.domain.model;

/**
 * 멀티히트 한 타의 결과를 나타내는 불변 record.
 *
 * <p>로그 브레이크다운 및 합산의 소스로 사용된다. 각 히트의 피해량과 크리티컬 여부를 보유한다.
 *
 * @param damage 해당 히트의 피해량 (1 이상)
 * @param critical 크리티컬 히트 여부
 */
public record HitResult(int damage, boolean critical) {}
