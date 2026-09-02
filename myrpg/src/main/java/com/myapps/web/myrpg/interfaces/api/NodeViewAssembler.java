package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.application.service.GatheringService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.service.MapViewFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 현재 노드 또는 활성 던전 방 기준 플레이 화면 전체 뷰 모델을 조립하는 컴포넌트.
 *
 * <p>캐릭터가 던전 내에 있는 경우 인스턴스 던전 그래프와 안개(Fog of War), 몬스터 및 나가기 상호작용 버튼을 조립하며, 일반 필드/마을에 있는 경우 기존 맵
 * 서비스 기반으로 뷰를 구성한다.
 */
@Component
public class NodeViewAssembler {

    private final MapService mapService;
    private final NpcService npcService;
    private final MonsterService monsterService;
    private final ProgressionService progressionService;
    private final PlayScreenViewHelper playScreenViewHelper;
    private final ActionLog actionLog;
    private final DungeonService dungeonService;
    private final MapViewFactory mapViewFactory;
    private final GatheringService gatheringService;

    /**
     * NodeViewAssembler를 생성한다.
     *
     * @param mapService 맵 데이터 서비스
     * @param npcService NPC 데이터 서비스
     * @param monsterService 몬스터 카탈로그 서비스
     * @param progressionService 환생 상태 조회 서비스
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     * @param actionLog 세션 보관 행동 로그
     * @param dungeonService 던전 관리 서비스
     * @param mapViewFactory 맵 뷰 생성 팩토리
     * @param gatheringService 채집 관리 서비스
     */
    public NodeViewAssembler(
            final MapService mapService,
            final NpcService npcService,
            final MonsterService monsterService,
            final ProgressionService progressionService,
            final PlayScreenViewHelper playScreenViewHelper,
            final ActionLog actionLog,
            final DungeonService dungeonService,
            final MapViewFactory mapViewFactory,
            final GatheringService gatheringService) {
        this.mapService = mapService;
        this.npcService = npcService;
        this.monsterService = monsterService;
        this.progressionService = progressionService;
        this.playScreenViewHelper = playScreenViewHelper;
        this.actionLog = actionLog;
        this.dungeonService = dungeonService;
        this.mapViewFactory = mapViewFactory;
        this.gatheringService = gatheringService;
    }

    /**
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * @param progress 캐릭터 진행상황
     * @return 플레이 화면 뷰 모델
     */
    public PlayScreenView fromProgress(final CharacterProgress progress) {
        return fromProgress(progress, null);
    }

    /**
     * 캐릭터 진행상황과 대화 대상(NPC 또는 몬스터)으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * @param progress 캐릭터 진행상황
     * @param talkTarget 대화 대상 (NPC 또는 몬스터, 없으면 null 또는 {@link TalkTarget#EMPTY})
     * @return 플레이 화면 뷰 모델
     */
    public PlayScreenView fromProgress(
            final CharacterProgress progress, final TalkTarget talkTarget) {
        final Long characterId = progress.getId();
        final Optional<DungeonInstance> dungeonOpt =
                dungeonService != null && characterId != null
                        ? dungeonService.getActiveDungeon(characterId)
                        : Optional.empty();

        if (dungeonOpt.isPresent()) {
            return buildDungeonView(progress, dungeonOpt.get(), talkTarget);
        }

        return buildFieldView(progress, talkTarget);
    }

    private PlayScreenView buildDungeonView(
            final CharacterProgress progress,
            final DungeonInstance dungeon,
            final TalkTarget talkTarget) {
        final MinimapView minimap = mapViewFactory.createMinimap(dungeon);
        final FullMapView fullMap = mapViewFactory.createFullMap(dungeon);
        final String currentRoomId = dungeon.currentRoomId();
        final DungeonRoomState roomState = dungeon.roomStates().get(currentRoomId);

        final List<InteractionItem> interactions = new ArrayList<>();
        if (currentRoomId.equals(dungeon.startRoomId())) {
            interactions.add(new InteractionItem("leave", "던전 나가기 🚪", false, "dungeon-leave", ""));
        }

        if (roomState != null && roomState.remainingMonsters() != null) {
            for (final String monsterId : roomState.remainingMonsters()) {
                monsterService
                        .byId(monsterId)
                        .ifPresent(
                                m ->
                                        interactions.add(
                                                new InteractionItem(
                                                        m.id(),
                                                        m.buttonLabel(),
                                                        false,
                                                        "monster",
                                                        m.id())));
            }
        }

        final List<ActionLogEntry> logs = actionLog.getEntries();
        final RebirthStatus status = progressionService.rebirthStatus(progress);
        final InfoPopupView info = playScreenViewHelper.buildInfo(progress, status);

        return assemblePlayScreen(progress, minimap, fullMap, interactions, talkTarget, logs, info);
    }

    private PlayScreenView buildFieldView(
            final CharacterProgress progress, final TalkTarget talkTarget) {
        final String currentNodeId = progress.getCurrentNodeId();
        final MapNode currentNode = mapService.node(currentNodeId);
        final MinimapView minimap = mapService.minimap(currentNodeId);
        final FullMapView fullMap = mapService.fullMap(currentNodeId);
        final List<ActionLogEntry> logs = actionLog.getEntries();

        final List<Npc> npcsOnNode = npcService.byNode(currentNodeId);
        final List<Monster> monstersOnNode = monsterService.byNode(currentNodeId);
        final List<InteractionItem> interactions =
                new ArrayList<>(playScreenViewHelper.buildInteractions(npcsOnNode, monstersOnNode));

        if (currentNode != null && currentNode.dungeonId() != null) {
            interactions.add(
                    new InteractionItem(
                            currentNode.dungeonId(),
                            "던전 입장 ⚔️",
                            false,
                            "dungeon-enter",
                            currentNode.dungeonId()));
        }

        if (gatheringService.isTreeAvailable(progress.getId(), currentNodeId)) {
            interactions.add(new InteractionItem("gather-wood", "나무", false, "gathering", "wood"));
        }

        final RebirthStatus status = progressionService.rebirthStatus(progress);
        final InfoPopupView info = playScreenViewHelper.buildInfo(progress, status);

        return assemblePlayScreen(progress, minimap, fullMap, interactions, talkTarget, logs, info);
    }

    private PlayScreenView assemblePlayScreen(
            final CharacterProgress progress,
            final MinimapView minimap,
            final FullMapView fullMap,
            final List<InteractionItem> interactions,
            final TalkTarget talkTarget,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        if (talkTarget != null && talkTarget.npc() != null) {
            return playScreenViewHelper.buildPlayScreen(
                    progress,
                    minimap,
                    fullMap,
                    interactions,
                    talkTarget.npc(),
                    talkTarget.dialogue(),
                    logs);
        }
        if (talkTarget != null) {
            return playScreenViewHelper.buildPlayScreen(
                    progress, minimap, fullMap, interactions, talkTarget, logs, info);
        }
        return playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, interactions, null, null, logs, info);
    }
}
