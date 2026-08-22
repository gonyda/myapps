package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 던전 메타데이터 및 맵 생성, 몬스터, 보스, 보상 스펙을 통합 정의하는 불변 레코드.
 *
 * @param id 던전 고유 식별자 (예: "alby", "ciar", "rabbie")
 * @param name 던전 표시 이름 (예: "알비 던전")
 * @param entranceNodeId 월드맵 입구 노드 ID (예: "alby-entrance")
 * @param theme 던전 테마/스타일 식별자
 * @param implemented 구현 완료 여부 (알비: true, 키아/라비: false)
 * @param generation 맵 생성 파라미터 스펙
 * @param monsterPool 일반 방 몬스터 출현 풀 목록
 * @param chainCombatProbability 일반 전투 승리 후 연쇄 전투 발동 확률 (예: 0.10)
 * @param boss 보스 몬스터 스펙
 * @param rewards 최종 보스 처치 클리어 보상 스펙
 */
public record DungeonSpec(
        String id,
        String name,
        String entranceNodeId,
        String theme,
        boolean implemented,
        DungeonGenerationSpec generation,
        List<DungeonMonsterEntry> monsterPool,
        double chainCombatProbability,
        DungeonBossSpec boss,
        DungeonRewardSpec rewards) {}
