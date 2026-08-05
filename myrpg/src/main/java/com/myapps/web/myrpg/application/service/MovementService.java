package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;

/**
 * 턴제 맵 이동을 처리하는 애플리케이션 서비스.
 *
 * <p>인접 노드 이동(방향 오프셋 기반)과 던전 내부 진입 요청을 검증하며,
 * 정상 흐름은 예외를 던지지 않고 sealed {@link MovementResult}로 결과를 반환한다.
 */
@Service
public class MovementService {

    private static final String BLOCKED_MESSAGE = "그곳으로는 갈 수 없습니다.";
    private static final String DUNGEON_LOCKED_MESSAGE = "아직 준비 중입니다.";

    private final MapService mapService;
    private final ActionLog actionLog;

    /**
     * MovementService를 생성한다.
     *
     * @param mapService 맵 노드 조회 및 그래프 제공 서비스
     * @param actionLog  행동 로그 (세션 보관)
     */
    public MovementService(final MapService mapService, final ActionLog actionLog) {
        this.mapService = mapService;
        this.actionLog = actionLog;
    }

    /**
     * 방향 오프셋(dx, dy)으로 인접 노드 이동을 시도한다.
     *
     * <p>현재 노드에서 좌표 오프셋에 해당하는 이웃 노드가 존재하고
     * {@code links}로 연결되어 있으면 이동에 성공하여 현재 노드를 갱신하고
     * 이동 로그를 생성한다. 그렇지 않으면 {@link MovementResult.Blocked}를 반환한다.
     *
     * @param progress 캐릭터 진행상황
     * @param dx       X 좌표 오프셋
     * @param dy       Y 좌표 오프셋
     * @return 이동 결과 ({@link MovementResult.Moved} 또는 {@link MovementResult.Blocked})
     */
    public MovementResult move(final CharacterProgress progress, final int dx, final int dy) {
        final MapNode currentNode = mapService.node(progress.getCurrentNodeId());
        final MapGraph graph = mapService.graph();

        final Optional<MapNode> neighborOpt = graph.neighborByOffset(currentNode, dx, dy);

        if (neighborOpt.isEmpty()) {
            return new MovementResult.Blocked(BLOCKED_MESSAGE);
        }

        final MapNode target = neighborOpt.get();

        if (!currentNode.links().contains(target.id())) {
            return new MovementResult.Blocked(BLOCKED_MESSAGE);
        }

        progress.updateCurrentNodeId(target.id());
        final ActionLogEntry logEntry = actionLog.add(target.name() + "(으)로 이동했습니다.", "move");

        return new MovementResult.Moved(target, logEntry);
    }

    /**
     * 던전 내부 진입을 시도한다.
     *
     * <p>현재 모든 던전은 준비 중이므로 항상 {@link MovementResult.DungeonLocked}를 반환한다.
     * 안내 문구 생성 실패와 무관하게 거부는 유지된다.
     *
     * @param progress  캐릭터 진행상황
     * @param dungeonId 진입하려는 던전 ID
     * @return 항상 {@link MovementResult.DungeonLocked}
     */
    public MovementResult enterDungeon(final CharacterProgress progress, final String dungeonId) {
        return new MovementResult.DungeonLocked(DUNGEON_LOCKED_MESSAGE);
    }
}
