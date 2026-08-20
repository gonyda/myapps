package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.MapNode;

/**
 * 맵 이동 요청의 결과를 나타내는 sealed 인터페이스.
 *
 * <p>정상 흐름은 예외를 던지지 않으며, 이동 불가/준비 중은 "정상적 거부"로 처리한다. 허용되는 구현체는 {@link Moved}, {@link Blocked},
 * {@link DungeonLocked}이다.
 */
public sealed interface MovementResult
        permits MovementResult.Moved, MovementResult.Blocked, MovementResult.DungeonLocked {

    /**
     * 인접 노드로 이동 성공 결과.
     *
     * <p>현재 노드 id가 대상 노드로 갱신되고, 이동 타입 로그가 생성된다.
     *
     * @param node 이동 목적지 노드
     * @param log 생성된 행동 로그 항목
     */
    record Moved(MapNode node, ActionLogEntry log) implements MovementResult {}

    /**
     * 비인접 노드 이동 요청 거부 결과.
     *
     * <p>캐릭터 상태는 변경되지 않는다.
     *
     * @param message 거부 안내 메시지
     */
    record Blocked(String message) implements MovementResult {}

    /**
     * 던전 내부 진입 거부 결과.
     *
     * <p>던전은 아직 준비 중이므로 진입이 허용되지 않는다. 안내 문구 생성 실패와 무관하게 거부는 유지된다.
     *
     * @param message 준비 중 안내 메시지
     */
    record DungeonLocked(String message) implements MovementResult {}
}
