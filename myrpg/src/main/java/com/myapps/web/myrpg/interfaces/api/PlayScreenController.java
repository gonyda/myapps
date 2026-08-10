package com.myapps.web.myrpg.interfaces.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MovementService;
import com.myapps.web.myrpg.application.service.NpcDialogueService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.TalentType;

/**
 * 플레이 화면을 서버사이드 렌더링하는 컨트롤러.
 *
 * <p>GET / 요청에 대해 캐릭터 진행상황, 미니맵, 전체지도,
 * 상황 멘트, 상호작용 목록, 행동 로그, 정보 팝업을 조합하여 {@code play} 뷰를 렌더링한다.
 * POST /move 요청으로 턴제 이동을 처리하고 갱신된 프래그먼트를 반환한다.
 * POST /npc/talk 요청으로 NPC 대화를 처리하고 센터 프래그먼트를 반환한다.
 * POST /exp/up, /exp/down, /rebirth 요청으로 경험치·사망·환생 진행을 처리한다.
 */
@Controller
public class PlayScreenController {

    private static final String NOTIFICATION_TYPE = "system";
    private static final String GROWTH_TYPE = "growth";
    private static final long TEST_EXP_AMOUNT = 500L;
    private static final long TEST_GOLD_AMOUNT = 100L;
    private static final long MINUTES_PER_HOUR = 60;

    private final CharacterService characterService;
    private final MapService mapService;
    private final AmbienceService ambienceService;
    private final MovementService movementService;
    private final NpcService npcService;
    private final NpcDialogueService npcDialogueService;
    private final ProgressionService progressionService;
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
     * @param progressionService   경험치/레벨업/사망/환생 서비스
     * @param actionLog            세션 보관 행동 로그
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     */
    public PlayScreenController(final CharacterService characterService,
                                final MapService mapService,
                                final AmbienceService ambienceService,
                                final MovementService movementService,
                                final NpcService npcService,
                                final NpcDialogueService npcDialogueService,
                                final ProgressionService progressionService,
                                final ActionLog actionLog,
                                final PlayScreenViewHelper playScreenViewHelper) {
        this.characterService = characterService;
        this.mapService = mapService;
        this.ambienceService = ambienceService;
        this.movementService = movementService;
        this.npcService = npcService;
        this.npcDialogueService = npcDialogueService;
        this.progressionService = progressionService;
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
     * 정보 팝업 내용만 반환한다 (동적 갱신용).
     *
     * <p>스킬 승급 등으로 스탯 보너스가 변경된 후 정보 팝업을 열 때
     * 최신 데이터를 반영하기 위해 사용한다.
     *
     * @param model Spring MVC 모델
     * @return info-content fragment 뷰 이름
     */
    @GetMapping("/info")
    public String infoContent(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/info-popup :: info-content";
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
     * 경험치 획득(테스트 버튼)을 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>고정 획득량({@code TEST_EXP_AMOUNT}) 만큼 경험치를 획득하고,
     * 레벨업 발생 시 피드백 로그를 추가한 뒤 진행 응답 프래그먼트를 반환한다.
     *
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/exp/up")
    public String expUp(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final LevelUpResult result = progressionService.gainExperience(progress, TEST_EXP_AMOUNT);
        characterService.saveTurn(progress);

        actionLog.add("경험치 " + TEST_EXP_AMOUNT + " 획득", GROWTH_TYPE);
        if (result.levelsGained() > 0) {
            actionLog.add("레벨업! Lv." + result.newLevel(), GROWTH_TYPE);
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 사망 패널티(테스트 버튼)를 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>사망 패널티를 적용하여 경험치를 차감하고,
     * 피드백 로그를 추가한 뒤 진행 응답 프래그먼트를 반환한다.
     *
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/exp/down")
    public String expDown(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final DeathResult result = progressionService.applyDeathPenalty(progress);
        characterService.saveTurn(progress);

        actionLog.add("사망 패널티: 경험치 -" + result.experienceLost(), NOTIFICATION_TYPE);

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 환생을 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>재능 파라미터를 파싱하고(누락/이상값은 MELEE 폴백), 환생을 시도한다.
     * 환생 성공 시 진행상황을 저장하고 선택 재능을 포함한 성공 로그를 추가한다.
     * 쿨다운 활성 시 저장하지 않고 남은 시간을 안내하는 로그를 추가한다.
     *
     * @param talentParam 재능 상수명 (누락/이상값 시 MELEE 폴백)
     * @param model       Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/rebirth")
    public String rebirth(@RequestParam(name = "talent", required = false) final String talentParam,
                          final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final TalentType talent = TalentType.fromNameOrFallback(talentParam, TalentType.MELEE);
        final RebirthResult result = progressionService.rebirth(progress, talent);

        if (result instanceof RebirthResult.Reborn) {
            characterService.saveTurn(progress);
            actionLog.add("환생했습니다 (재능: " + talent.label() + ")", NOTIFICATION_TYPE);
        } else if (result instanceof RebirthResult.CooldownActive cooldown) {
            final Duration remaining = cooldown.remaining();
            final long hours = remaining.toHours();
            final long minutes = remaining.toMinutes() % MINUTES_PER_HOUR;
            actionLog.add("환생까지 " + hours + "시간 " + minutes + "분 남았습니다", NOTIFICATION_TYPE);
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 골드 획득(임시 버튼)을 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>실제 획득/소모 경로(몬스터 5·6순위, 아이템 판매/상점 7순위) 구현 시 제거될 임시 엔드포인트.
     * 고정 획득량({@code TEST_GOLD_AMOUNT}) 만큼 골드를 획득하고 행동 로그에 기록한다.
     *
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/gold/gain")
    public String goldGain(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        progress.gainGold(TEST_GOLD_AMOUNT);
        characterService.saveTurn(progress);

        actionLog.add("골드 " + TEST_GOLD_AMOUNT + " 획득", GROWTH_TYPE);

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 골드 소모(임시 버튼)를 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>실제 획득/소모 경로(몬스터 5·6순위, 아이템 판매/상점 7순위) 구현 시 제거될 임시 엔드포인트.
     * 고정 소모량({@code TEST_GOLD_AMOUNT}) 만큼 골드를 차감하고 행동 로그에 기록한다.
     * 소지금이 부족하면 차감하지 않고 부족 안내를 로그에 기록한다.
     *
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/gold/spend")
    public String goldSpend(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();

        try {
            progress.spendGold(TEST_GOLD_AMOUNT);
            characterService.saveTurn(progress);
            actionLog.add("골드 " + TEST_GOLD_AMOUNT + " 소모", GROWTH_TYPE);
        } catch (InsufficientGoldException e) {
            actionLog.add("골드가 부족합니다", NOTIFICATION_TYPE);
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * <p>현재 노드의 NPC 목록을 조회하여 상호작용 버튼을 구성하고,
     * 환생 상태를 조회하여 정보 팝업을 조립한 뒤 뷰를 반환한다.
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

        final RebirthStatus status = progressionService.rebirthStatus(progress);
        final InfoPopupView info = playScreenViewHelper.buildInfo(progress, status);

        return playScreenViewHelper.buildPlayScreen(
                progress, minimap, fullMap, ambience, interactions, null, null, logs, info);
    }
}
