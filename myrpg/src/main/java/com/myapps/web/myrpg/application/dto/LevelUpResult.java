package com.myapps.web.myrpg.application.dto;

/**
 * 레벨업 결과를 나타내는 DTO.
 *
 * <p>경험치 획득으로 인해 발생한 레벨업 횟수와 최종 레벨을 포함한다.
 *
 * @param levelsGained 이번 경험치 획득으로 올라간 레벨 수
 * @param newLevel     레벨업 후 최종 레벨
 */
public record LevelUpResult(int levelsGained, int newLevel) {
}
