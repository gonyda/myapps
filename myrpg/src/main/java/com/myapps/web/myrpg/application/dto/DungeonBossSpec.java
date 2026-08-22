package com.myapps.web.myrpg.application.dto;

/**
 * 던전 최종 보스 몬스터 설정을 나타내는 불변 레코드.
 *
 * @param monsterId 보스 몬스터 ID (MonsterService 카탈로그 참조)
 * @param name 보스 몬스터 표시 이름
 * @param dialogue 보스 조우 시 출력 대사
 */
public record DungeonBossSpec(String monsterId, String name, String dialogue) {}
