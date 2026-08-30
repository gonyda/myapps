package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 턴제 맵 이동을 처리하는 애플리케이션 서비스.
 *
 * <p>인접 노드 이동(방향 오프셋 기반)과 던전 내부 진입 요청을 검증하며, 정상 흐름은 예외를 던지지 않고 sealed {@link MovementResult}로 결과를
 * 반환한다. 캐릭터가 인스턴스 던전 내에 있는 경우 던전 격자 그래프 기반으로 방 이동을 수행한다.
 */
@Service
public class MovementService {

    private final MapService mapService;
    private final ActionLog actionLog;
    private final DungeonService dungeonService;
    private final GameProperties gameProperties;
    private final GameMessageService gameMessageService;

    /**
     * MovementService를 생성한다 (Spring 주입용).
     *
     * @param mapService 맵 노드 조회 및 그래프 제공 서비스
     * @param actionLog 행동 로그 (세션 보관)
     * @param dungeonService 던전 인스턴스 및 이동 관리 서비스
     * @param gameProperties 게임 밸런스 설정 프로퍼티
     * @param gameMessageService 메시지 서비스
     */
    @org.springframework.beans.factory.annotation.Autowired
    public MovementService(
            final MapService mapService,
            final ActionLog actionLog,
            final DungeonService dungeonService,
            final GameProperties gameProperties,
            final GameMessageService gameMessageService) {
        this.mapService = mapService;
        this.actionLog = actionLog;
        this.dungeonService = dungeonService;
        this.gameProperties = gameProperties;
        this.gameMessageService =
                gameMessageService != null ? gameMessageService : new GameMessageService(null);
    }

    /** 이전 호환용 생성자. */
    public MovementService(
            final MapService mapService,
            final ActionLog actionLog,
            final DungeonService dungeonService) {
        this(
                mapService,
                actionLog,
                dungeonService,
                new GameProperties(null, null, null, null, null, null),
                new GameMessageService(null));
    }

    /**
     * 방향 오프셋(dx, dy)으로 인접 노드 또는 던전 방 이동을 시도한다.
     *
     * <p>캐릭터가 활성 던전에 있는 경우 던전 그래프의 인접 방으로 이동하며, 필드/마을에 있는 경우 월드 맵 그래프의 인접 노드로 이동한다.
     *
     * @param progress 캐릭터 진행상황
     * @param dx X 좌표 오프셋
     * @param dy Y 좌표 오프셋
     * @return 이동 결과 ({@link MovementResult.Moved} 또는 {@link MovementResult.Blocked})
     */
    public MovementResult move(final CharacterProgress progress, final int dx, final int dy) {
        final Long characterId = progress.getId();
        final Optional<DungeonInstance> activeDungeonOpt =
                dungeonService != null && characterId != null
                        ? dungeonService.getActiveDungeon(characterId)
                        : Optional.empty();

        if (activeDungeonOpt.isPresent()) {
            return moveInDungeon(characterId, activeDungeonOpt.get(), dx, dy);
        }

        return moveInWorld(progress, dx, dy);
    }

    private MovementResult moveInDungeon(
            final Long characterId, final DungeonInstance dungeon, final int dx, final int dy) {
        final String currentRoomId = dungeon.currentRoomId();
        final MapGraph dungeonGraph = dungeon.dungeonGraph();
        final Optional<MapNode> currentRoomOpt = dungeonGraph.byId(currentRoomId);

        if (currentRoomOpt.isEmpty()) {
            return new MovementResult.Blocked(gameMessageService.get("movement.blocked"));
        }

        final MapNode currentRoom = currentRoomOpt.get();
        final Optional<MapNode> neighborOpt = dungeonGraph.neighborByOffset(currentRoom, dx, dy);

        if (neighborOpt.isEmpty()) {
            return new MovementResult.Blocked(gameMessageService.get("movement.blocked"));
        }

        final MapNode targetRoom = neighborOpt.get();
        if (!currentRoom.links().contains(targetRoom.id())) {
            return new MovementResult.Blocked(gameMessageService.get("movement.blocked"));
        }

        try {
            dungeonService.moveToRoom(characterId, targetRoom.id());
            return new MovementResult.Moved(targetRoom, null);
        } catch (final BlockedMovementException e) {
            return new MovementResult.Blocked(e.getMessage());
        }
    }

    private MovementResult moveInWorld(
            final CharacterProgress progress, final int dx, final int dy) {
        final MapNode currentNode = mapService.node(progress.getCurrentNodeId());
        final MapGraph graph = mapService.graph();

        final Optional<MapNode> neighborOpt = graph.neighborByOffset(currentNode, dx, dy);

        if (neighborOpt.isEmpty()) {
            return new MovementResult.Blocked(gameMessageService.get("movement.blocked"));
        }

        final MapNode target = neighborOpt.get();

        if (!currentNode.links().contains(target.id())) {
            return new MovementResult.Blocked(gameMessageService.get("movement.blocked"));
        }

        final int moveMinutes = gameProperties.movement().worldMoveMinutes();
        progress.advanceInGameTime(moveMinutes);
        progress.updateCurrentNodeId(target.id());

        return new MovementResult.Moved(target, null);
    }

    /**
     * 던전 내부 진입을 시도한다.
     *
     * <p>현재 모든 던전은 준비 중이므로 항상 {@link MovementResult.DungeonLocked}를 반환한다. 안내 문구 생성 실패와 무관하게 거부는
     * 유지된다.
     *
     * @param progress 캐릭터 진행상황
     * @param dungeonId 진입하려는 던전 ID
     * @return 항상 {@link MovementResult.DungeonLocked}
     */
    public MovementResult enterDungeon(final CharacterProgress progress, final String dungeonId) {
        return new MovementResult.DungeonLocked(gameMessageService.get("movement.dungeon_locked"));
    }
}
