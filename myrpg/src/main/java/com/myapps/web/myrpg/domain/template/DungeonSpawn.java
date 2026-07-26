package com.myapps.web.myrpg.domain.template;

/**
 * 던전 내 몬스터 출현 정보를 정의하는 레코드.
 *
 * <p>특정 몬스터가 어느 층 범위에서 어떤 가중치로 출현하는지를 나타낸다.
 * DungeonTemplate의 monsters 리스트 요소로 사용된다.
 */
public record DungeonSpawn(long monsterId, int minFloor, int maxFloor, int spawnWeight) {
}
