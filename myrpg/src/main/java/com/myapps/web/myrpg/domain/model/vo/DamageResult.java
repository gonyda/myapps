package com.myapps.web.myrpg.domain.model.vo;

/**
 * 데미지 산출 결과를 나타내는 값 객체.
 *
 * <p>최종 데미지 수치와 치명타 발동 여부를 포함한다.
 */
public record DamageResult(int damage, boolean critical) {
}
