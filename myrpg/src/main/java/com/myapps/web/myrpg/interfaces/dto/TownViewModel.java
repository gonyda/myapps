package com.myapps.web.myrpg.interfaces.dto;

/**
 * 마을(메인) 화면에 전달할 뷰 모델.
 *
 * <p>플레이어의 현재 상태(레벨, HP, MP, 골드)와 진행 중인 던전 존재 여부를 표현한다.
 */
public record TownViewModel(String playerName, int playerLevel,
                            int hp, int maxHp, int mp, int maxMp,
                            int gold, boolean hasActiveRun) {
}
