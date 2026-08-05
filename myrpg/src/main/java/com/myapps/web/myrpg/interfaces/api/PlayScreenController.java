package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MovementService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;

/**
 * 플레이 화면을 서버사이드 렌더링하는 컨트롤러.
 *
 * <p>GET / 요청에 대해 캐릭터 진행상황, 미니맵, 전체지도,
 * 상황 멘트, 행동 로그를 조합하여 {@code play} 뷰를 렌더링한다.
 * POST /move 요청으로 턴제 이동을 처리하고 갱신된 프래그먼트를 반환한다.
 */
@Controller
public class PlayScreenController {

    private static final String NOTIFICATION_TYPE = "system";

    private final CharacterService characterService;
    private final MapService mapService;
    private final AmbienceService ambienceService;
    private final MovementService movementService;
    private final ActionLog actionLog;
    private final PlayScreenViewHelper playScreenViewHelper;

    /**
     * PlayScreenController를 생성한다.
     *
     * @param characterService     캐릭터 서비스
     * @param mapService           맵 서비스
     * @param ambienceService      상황 멘트 서비스
     * @param movementService      이동 처리 서비스
     * @param actionLog            세션 보관 행동 로그
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     */
    public PlayScreenController(final CharacterService characterService,
                                final MapService mapService,
                                final AmbienceService ambienceService,
                                final MovementService movementService,
                                final ActionLog actionLog,
                                final PlayScreenViewHelper playScreenViewHelper) {
        this.characterService = characterService;
        this.mapService = mapService;
        this.ambienceService = ambienceService;
        this.movementService = movementService;
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
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
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

        return playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, ambience, logs);
    }
}
