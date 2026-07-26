package com.myapps.web.myrpg.domain.model.vo;

/**
 * 레벨업 처리 결과를 나타내는 값 객체.
 *
 * <p>새 레벨, 상승한 레벨 수, 남은 경험치를 포함한다.
 */
public record LevelUpResult(int newLevel, int levelsGained, int remainingExp) {
}
