package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.Npc;

/**
 * 현재 노드 기준 플레이 화면 전체 뷰 모델을 조립하는 컴포넌트.
 *
 * <p>캐릭터의 현재 노드에서 미니맵·전체지도·상황 멘트·상호작용(NPC+몬스터)·
 * 행동 로그·정보 팝업을 모아 {@link PlayScreenView}를 생성한다.
 *
 * <p>플레이 화면 진입({@code GET /})과 전투 종료 후 화면 복원에서 동일한 뷰를
 * 재사용하기 위해 분리되었다. 전투 종료 시 이 뷰를 사용하지 않으면 상호작용
 * 버튼(예: 몬스터 조우 버튼)이 사라지므로, 두 경로가 반드시 같은 조립을 공유해야 한다.
 */
@Component
public class NodeViewAssembler {

    private final MapService mapService;
    private final AmbienceService ambienceService;
    private final NpcService npcService;
    private final MonsterService monsterService;
    private final ProgressionService progressionService;
    private final PlayScreenViewHelper playScreenViewHelper;
    private final ActionLog actionLog;

    /**
     * NodeViewAssembler를 생성한다.
     *
     * @param mapService           맵 데이터 서비스 (노드/미니맵/전체지도)
     * @param ambienceService      상황 멘트 서비스
     * @param npcService           NPC 데이터 서비스
     * @param monsterService       몬스터 카탈로그 서비스
     * @param progressionService   환생 상태 조회 서비스
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     * @param actionLog            세션 보관 행동 로그
     */
    public NodeViewAssembler(final MapService mapService,
                             final AmbienceService ambienceService,
                             final NpcService npcService,
                             final MonsterService monsterService,
                             final ProgressionService progressionService,
                             final PlayScreenViewHelper playScreenViewHelper,
                             final ActionLog actionLog) {
        this.mapService = mapService;
        this.ambienceService = ambienceService;
        this.npcService = npcService;
        this.monsterService = monsterService;
        this.progressionService = progressionService;
        this.playScreenViewHelper = playScreenViewHelper;
        this.actionLog = actionLog;
    }

    /**
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * <p>현재 노드의 NPC·몬스터 목록으로 상호작용 버튼을 구성하고,
     * 환생 상태를 조회하여 정보 팝업을 조립한 뒤 뷰를 반환한다.
     *
     * @param progress 캐릭터 진행상황
     * @return 플레이 화면 뷰 모델
     */
    public PlayScreenView fromProgress(final CharacterProgress progress) {
        final String currentNodeId = progress.getCurrentNodeId();
        final MapNode currentNode = mapService.node(currentNodeId);
        final MinimapView minimap = mapService.minimap(currentNodeId);
        final FullMapView fullMap = mapService.fullMap(currentNodeId);
        final String ambience = ambienceService.ambience(currentNode);
        final List<ActionLogEntry> logs = actionLog.getEntries();

        final List<Npc> npcsOnNode = npcService.byNode(currentNodeId);
        final List<Monster> monstersOnNode = monsterService.byNode(currentNodeId);
        final List<InteractionItem> interactions =
                playScreenViewHelper.buildInteractions(npcsOnNode, monstersOnNode);

        final RebirthStatus status = progressionService.rebirthStatus(progress);
        final InfoPopupView info = playScreenViewHelper.buildInfo(progress, status);

        return playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, ambience, interactions, null, null, logs, info);
    }
}
