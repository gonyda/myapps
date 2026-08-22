package com.myapps.web.myrpg.application.dto;

/**
 * 던전 일반 방 몬스터 출현 풀 항목을 나타내는 불변 레코드.
 *
 * @param monsterId 출현 몬스터 ID (MonsterService 카탈로그 참조)
 * @param minCount 해당 몬스터 최소 스폰 마리수 (1 이상)
 * @param maxCount 해당 몬스터 최대 스폰 마리수 (minCount 이상)
 * @param weight 가중치 기반 출현 확률 가중치 (1 이상)
 */
public record DungeonMonsterEntry(String monsterId, int minCount, int maxCount, int weight) {}
