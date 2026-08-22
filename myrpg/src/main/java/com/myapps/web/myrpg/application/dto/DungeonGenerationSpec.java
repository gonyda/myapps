package com.myapps.web.myrpg.application.dto;

/**
 * 던전 맵 프로시저럴 생성 파라미터를 나타내는 불변 레코드.
 *
 * @param minDistanceToBoss 시작방에서 보스방까지의 최소 거리 (최단 경로 홉 수)
 * @param maxDistanceToBoss 시작방에서 보스방까지의 최대 거리 (최단 경로 홉 수)
 * @param minTotalRooms 던전 전체 최소 방 개수
 * @param maxTotalRooms 던전 전체 최대 방 개수
 * @param branchProbability 주 경로 노드에서 서브 브랜치가 분기될 확률 (0.0 ~ 1.0)
 * @param maxBranchDepth 서브 브랜치의 최대 깊이 (막다른 길 길이)
 */
public record DungeonGenerationSpec(
        int minDistanceToBoss,
        int maxDistanceToBoss,
        int minTotalRooms,
        int maxTotalRooms,
        double branchProbability,
        int maxBranchDepth) {}
