package com.myapps.web.myrpg.interfaces.api;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MovementService;
import com.myapps.web.myrpg.application.service.NpcDialogueService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Npc;

/**
 * 플레이 화면을 서버사이드 렌더링하는 컨트롤러.
 *
 * <p>GET / 요청에 대해 캐릭터 진행상황, 미니맵, 전체지도,
 * 상황 멘트, 상호작용 목록, 행동 로그를 조합하여 {@code play} 뷰를 렌더링한다.
 * POST /move 요청으로 턴제 이동을 처리하고 갱신된 프래그먼트를 반환한다.
 * POST /npc/talk 요청으로 NPC 대화를 처리하고 센터 프래그먼트를 반환한다.
 */
@Controller
public class PlayScreenController {

    private static final String NOTIFICATION_TYPE = "system";

    private final CharacterService characterService;
    private final MapService mapService;
    private final AmbienceService ambienceService;
    private final MovementService movementService;
    private final NpcService npcService;
    private final NpcDialogueService npcDialogueService;
    private final ActionLog actionLog;
    private final PlayScreenViewHelper playScreenViewHelper;

    /**
     * PlayScreenController를 생성한다.
     *
     * @param characterService     캐릭터 서비스
     * @param mapService           맵 서비스
     * @param ambienceService      상황 멘트 서비스
     * @param movementService      이동 처리 서비스
     * @param npcService           NPC 데이터 서비스
     * @param npcDialogueService   NPC 대사 선택 서비스
     * @param actionLog            세션 보관 행동 로그
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     */
    public PlayScreenController(final CharacterService characterService,
                                final MapService mapService,
                                final AmbienceService ambienceService,
                                final MovementService movementService,
                                final NpcService npcService,
                                final NpcDialogueService npcDialogueService,
                                final ActionLog actionLog,
                                final PlayScreenViewHelper playScreenViewHelper) {
        this.characterService = characterService;
        this.mapService = mapService;
        this.ambienceService = ambienceService;
        this.movementService = movementService;
        this.npcService = npcService;
        this.npcDialogueService = npcDialogueService;
        this.actionLog = actionLog;
        this.playScreenViewHelper = playScreenViewHelper;
    }

    /**
     * 플레이 화면을 렌더링한다.
     *
     * <p>캐릭터 진행상황을 로드(또는 기본 생성)하고,
     * 현재 노드 기준의 미니맵/전체지도/상황 멘트/행동 로그를 조합하여
     * 모델에 추가한 뒤 {@code play} 뷰를 반환한다.
     *
     * @param model Spring MVC 모델
     * @return 뷰 이름 {@code "play"}
     */
    @GetMapping("/")
    public String playScreen(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "play";
    }

    /**
     * 턴제 이동을 처리하고 갱신된 HTML 프래그먼트를 반환한다.
     *
     * <p>이동 성공 시 캐릭터 진행상황을 저장한 뒤 갱신된 top-bar, center,
     * action-log 프래그먼트를 반환한다. 이동 거부 시 안내 로그만 추가하고
     * 동일 프래그먼트를 반환한다.
     *
     * @param dx    X 좌표 오프셋
     * @param dy    Y 좌표 오프셋
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/move-response"}
     */
    @PostMapping("/move")
    public String move(@RequestParam final int dx,
                       @RequestParam final int dy,
                       final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final MovementResult result = movementService.move(progress, dx, dy);

        if (result instanceof MovementResult.Moved) {
            characterService.saveTurn(progress);
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/move-response";
    }

    /**
     * NPC 대화를 처리하고 센터 프래그먼트를 반환한다.
     *
     * <p>지정된 NPC와의 대화를 시작하여 NPC 이름·대사·행동 버튼을
     * 포함한 센터 영역을 완전히 교체하는 프래그먼트를 반환한다.
     * 현재 노드의 상호작용 목록도 함께 재구성된다.
     *
     * @param npcId 대화 대상 NPC ID
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/npc-response"}
     */
    @PostMapping("/npc/talk")
    public String talkToNpc(@RequestParam final String npcId,
                            final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final String currentNodeId = progress.getCurrentNodeId();

        final List<Npc> npcsOnNode = npcService.byNode(currentNodeId);
        final List<InteractionItem> interactions = playScreenViewHelper.buildInteractions(npcsOnNode);

        final Optional<Npc> targetNpc = npcService.byId(npcId);
        final Npc talkingNpc = targetNpc.orElse(null);
        final String dialogue = talkingNpc != null
                ? npcDialogueService.selectLine(talkingNpc) : null;

        final MapNode currentNode = mapService.node(currentNodeId);
        final MinimapView minimap = mapService.minimap(currentNodeId);
        final FullMapView fullMap = mapService.fullMap(currentNodeId);
        final String ambience = ambienceService.ambience(currentNode);
        final List<ActionLogEntry> logs = actionLog.getEntries();

        final PlayScreenView view = playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, ambience, interactions, talkingNpc, dialogue, logs);
        model.addAttribute("view", view);
        return "fragments/npc-response";
    }

    /**
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * <p>현재 노드의 NPC 목록을 조회하여 상호작용 버튼을 구성하고,
     * 대사·행동 버튼은 비운 상태로 뷰를 조립한다.
     *
     * @param progress 캐릭터 진행상황
     * @return 플레이 화면 뷰 모델
     */
    private PlayScreenView buildViewFromProgress(final CharacterProgress progress) {
        final String currentNodeId = progress.getCurrentNodeId();
        final MapNode currentNode = mapService.node(currentNodeId);
        final MinimapView minimap = mapService.minimap(currentNodeId);
        final FullMapView fullMap = mapService.fullMap(currentNodeId);
        final String ambience = ambienceService.ambience(currentNode);
        final List<ActionLogEntry> logs = actionLog.getEntries();

        final List<Npc> npcsOnNode = npcService.byNode(currentNodeId);
        final List<InteractionItem> interactions = playScreenViewHelper.buildInteractions(npcsOnNode);

        return playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, ambience, interactions, null, null, logs);
    }
}
