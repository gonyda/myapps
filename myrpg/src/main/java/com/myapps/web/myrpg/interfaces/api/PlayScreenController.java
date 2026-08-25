package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterDialogueService;
import com.myapps.web.myrpg.application.service.MonsterEncounterService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.application.service.MovementService;
import com.myapps.web.myrpg.application.service.NpcDialogueService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.TalentType;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 플레이 화면을 서버사이드 렌더링하는 컨트롤러.
 *
 * <p>GET / 요청에 대해 캐릭터 진행상황, 미니맵, 전체지도, 상황 멘트, 상호작용 목록, 행동 로그, 정보 팝업을 조합하여 {@code play} 뷰를 렌더링한다.
 * POST /move 요청으로 턴제 이동을 처리하고 갱신된 프래그먼트를 반환한다. POST /npc/talk 요청으로 NPC 대화를 처리하고 센터 프래그먼트를 반환한다.
 * POST /rebirth 요청으로 환생 진행을 처리한다.
 */
@Controller
public class PlayScreenController {

    private static final String NOTIFICATION_TYPE = "system";
    private static final String COMBAT_TYPE = "combat";
    private static final long MINUTES_PER_HOUR = 60;

    private final CharacterService characterService;
    private final MapService mapService;
    private final AmbienceService ambienceService;
    private final MovementService movementService;
    private final NpcService npcService;
    private final NpcDialogueService npcDialogueService;
    private final ProgressionService progressionService;
    private final MonsterService monsterService;
    private final MonsterDialogueService monsterDialogueService;
    private final MonsterEncounterService monsterEncounterService;
    private final BattleService battleService;
    private final ActionLog actionLog;
    private final PlayScreenViewHelper playScreenViewHelper;
    private final NodeViewAssembler nodeViewAssembler;
    private final com.myapps.web.myrpg.application.service.DungeonService dungeonService;

    /**
     * PlayScreenController를 생성한다.
     *
     * @param characterService 캐릭터 서비스
     * @param mapService 맵 서비스
     * @param ambienceService 상황 멘트 서비스
     * @param movementService 이동 처리 서비스
     * @param npcService NPC 데이터 서비스
     * @param npcDialogueService NPC 대사 선택 서비스
     * @param progressionService 경험치/레벨업/사망/환생 서비스
     * @param monsterService 몬스터 카탈로그 조회 서비스
     * @param monsterDialogueService 몬스터 조우 대사 선택 서비스
     * @param monsterEncounterService 필드 진입 선공 판정 서비스
     * @param battleService 전투 오케스트레이션 서비스
     * @param actionLog 세션 보관 행동 로그
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     * @param nodeViewAssembler 현재 노드 기준 플레이 화면 뷰 조립 컴포넌트
     * @param dungeonService 던전 서비스
     */
    public PlayScreenController(
            final CharacterService characterService,
            final MapService mapService,
            final AmbienceService ambienceService,
            final MovementService movementService,
            final NpcService npcService,
            final NpcDialogueService npcDialogueService,
            final ProgressionService progressionService,
            final MonsterService monsterService,
            final MonsterDialogueService monsterDialogueService,
            final MonsterEncounterService monsterEncounterService,
            final BattleService battleService,
            final ActionLog actionLog,
            final PlayScreenViewHelper playScreenViewHelper,
            final NodeViewAssembler nodeViewAssembler,
            final com.myapps.web.myrpg.application.service.DungeonService dungeonService) {
        this.characterService = characterService;
        this.mapService = mapService;
        this.ambienceService = ambienceService;
        this.movementService = movementService;
        this.npcService = npcService;
        this.npcDialogueService = npcDialogueService;
        this.progressionService = progressionService;
        this.monsterService = monsterService;
        this.monsterDialogueService = monsterDialogueService;
        this.monsterEncounterService = monsterEncounterService;
        this.battleService = battleService;
        this.actionLog = actionLog;
        this.playScreenViewHelper = playScreenViewHelper;
        this.nodeViewAssembler = nodeViewAssembler;
        this.dungeonService = dungeonService;
    }

    /**
     * 플레이 화면을 렌더링한다.
     *
     * <p>캐릭터 진행상황을 로드(또는 기본 생성)하고, 활성 전투가 있으면 전투 뷰를 복원하여 {@code battleActive=true}로 진입시킨다. 없으면 현재
     * 노드 기준의 미니맵/전체지도/상황 멘트/행동 로그를 조합하여 모델에 추가한 뒤 {@code play} 뷰를 반환한다.
     *
     * @param model Spring MVC 모델
     * @return 뷰 이름 {@code "play"}
     */
    /**
     * 플레이 화면을 렌더링한다.
     *
     * <p>캐릭터 진행상황을 로드(또는 기본 생성)하고, 활성 전투가 있으면 전투 뷰를 복원하여 {@code battleActive=true}로 진입시킨다. 없으면 현재
     * 노드 기준의 미니맵/전체지도/상황 멘트/행동 로그를 조합하여 모델에 추가한 뒤 {@code play} 뷰를 반환한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 뷰 이름 {@code "play"}
     */
    @GetMapping("/")
    public String playScreen(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final Optional<BattleState> activeBattle = battleService.resumeIfActive(progress);

        if (activeBattle.isPresent()) {
            final BattleState state = activeBattle.get();
            final Optional<Monster> monsterOpt = monsterService.byId(state.getMonsterId());
            if (monsterOpt.isPresent()) {
                final Monster monster = monsterOpt.get();
                final List<BattleSkillButton> skills = battleService.combatSkills(progress);
                final BattleView battleView =
                        new BattleView(
                                monster.name(),
                                monster.level(),
                                state.getMonsterCurrentHp(),
                                monster.maxHp(),
                                skills,
                                true,
                                state.isStandby(),
                                state.getCurrentMonsterIntent(),
                                0,
                                null,
                                null,
                                false);
                final PlayScreenView view = buildViewFromProgress(progress);
                model.addAttribute("view", view);
                model.addAttribute("battleView", battleView);
                model.addAttribute("skills", skills);
                model.addAttribute("battleActive", true);
                model.addAttribute(
                        "turnLog", List.of(monster.name() + " Lv." + monster.level() + " 출현!"));
                return "play";
            }
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "play";
    }

    /**
     * 정보 팝업 내용만 반환한다 (동적 갱신용).
     *
     * <p>스킬 승급 등으로 스탯 보너스가 변경된 후 정보 팝업을 열 때 최신 데이터를 반영하기 위해 사용한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return info-content fragment 뷰 이름
     */
    @GetMapping("/info")
    public String infoContent(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/info-popup :: info-content";
    }

    /**
     * 턴제 이동을 처리하고 갱신된 HTML 프래그먼트를 반환한다.
     *
     * <p>활성 전투가 있으면 이동을 거부(방어적)한다. 이동 성공 시 캐릭터 진행상황을 저장한 뒤, 해당 노드의 몬스터를 검사하여 5% 기습 판정을 수행한다. 기습 발동
     * 시 자동으로 전투를 시작하고 {@code #ambushSignal}(몬스터명)을 응답에 포함한다. 이동 거부 시 안내 로그만 추가하고 동일 프래그먼트를 반환한다.
     *
     * @param dx X 좌표 오프셋
     * @param dy Y 좌표 오프셋
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/move-response"}
     */
    @PostMapping("/move")
    public String move(
            @RequestParam final int dx,
            @RequestParam final int dy,
            final HttpSession session,
            final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);

        final Optional<BattleState> activeBattle = battleService.resumeIfActive(progress);
        if (activeBattle.isPresent()) {
            actionLog.add("전투 중에는 이동할 수 없습니다.", NOTIFICATION_TYPE);
            final PlayScreenView view = buildViewFromProgress(progress);
            model.addAttribute("view", view);
            return "fragments/move-response";
        }

        final MovementResult result = movementService.move(progress, dx, dy);

        if (result instanceof MovementResult.Moved) {
            final boolean isInDungeon =
                    dungeonService != null
                            && progress.getId() != null
                            && dungeonService.getActiveDungeon(progress.getId()).isPresent();

            if (!isInDungeon) {
                characterService.saveTurn(progress);

                final List<Monster> monstersOnNode =
                        monsterService.byNode(progress.getCurrentNodeId());
                final Optional<Monster> ambusher =
                        monsterEncounterService.rollPreemptiveStrike(monstersOnNode);
                ambusher.ifPresent(
                        monster -> {
                            battleService.start(progress, monster.id(), true);
                            actionLog.add("🚨 " + monster.name() + " 기습!", COMBAT_TYPE);
                            model.addAttribute("ambushMonsterName", monster.name());
                        });
            }
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/move-response";
    }

    /**
     * NPC 대화를 처리하고 센터 프래그먼트를 반환한다.
     *
     * <p>지정된 NPC와의 대화를 시작하여 NPC 이름·대사·행동 버튼을 포함한 센터 영역을 완전히 교체하는 프래그먼트를 반환한다. 현재 노드의 상호작용 목록도 함께
     * 재구성된다.
     *
     * @param npcId 대화 대상 NPC ID
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/npc-response"}
     */
    @PostMapping("/npc/talk")
    public String talkToNpc(
            @RequestParam final String npcId, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);

        final Optional<Npc> targetNpc = npcService.byId(npcId);
        final String dialogue =
                targetNpc.isPresent() ? npcDialogueService.selectLine(targetNpc.get()) : null;
        final TalkTarget talkTarget =
                targetNpc.map(npc -> TalkTarget.ofNpc(npc, dialogue)).orElse(TalkTarget.EMPTY);

        final PlayScreenView view = nodeViewAssembler.fromProgress(progress, talkTarget);
        model.addAttribute("view", view);
        model.addAttribute("talkingNpcId", npcId);
        return "fragments/npc-response";
    }

    /**
     * 몬스터 조우를 처리하고 센터 프래그먼트를 반환한다.
     *
     * <p>지정된 몬스터와의 조우를 시작하여 몬스터 이름·레벨·HP·대사·행동 버튼을 포함한 센터 영역을 완전히 교체하는 프래그먼트를 반환한다. 현재 노드의 상호작용
     * 목록(NPC+몬스터)도 함께 재구성된다.
     *
     * <p>미지 몬스터 ID이거나 현재 노드에 배치되지 않은 몬스터를 요청하면 예외 없이 대사·행동 버튼을 비운 채 정상 렌더된다(관용 설계).
     *
     * <p>현재는 조우 대사와 {@code 전투} 플레이스홀더 버튼만 표시한다. 6순위(전투 시스템) 구현 시 {@code 전투} 버튼이 {@code POST
     * /battle/start}로 교체되어 실제 전투 턴 진입·데미지 계산·드랍 지급 흐름을 시작하게 된다.
     *
     * @param monsterId 조우 대상 몬스터 ID
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/monster-response"}
     */
    @PostMapping("/monster/encounter")
    public String encounterMonster(
            @RequestParam final String monsterId, final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);

        final Optional<Monster> targetMonster = monsterService.byId(monsterId);

        final TalkTarget talkTarget;
        if (targetMonster.isPresent()) {
            final Monster monster = targetMonster.get();
            final String dialogue = monsterDialogueService.selectLine(monster);
            talkTarget = TalkTarget.ofMonster(monster, dialogue);
            model.addAttribute("encounteredMonsterId", monster.id());
        } else {
            talkTarget = TalkTarget.EMPTY;
        }

        final PlayScreenView view = nodeViewAssembler.fromProgress(progress, talkTarget);
        model.addAttribute("view", view);
        return "fragments/monster-response";
    }

    /**
     * 환생을 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * <p>재능 파라미터를 파싱하고(누락/이상값은 MELEE 폴백), 환생을 시도한다. 환생 성공 시 진행상황을 저장하고 선택 재능을 포함한 성공 로그를 추가한다. 쿨다운
     * 활성 시 저장하지 않고 남은 시간을 안내하는 로그를 추가한다.
     *
     * @param talentParam 재능 상수명 (누락/이상값 시 MELEE 폴백)
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/rebirth")
    public String rebirth(
            @RequestParam(name = "talent", required = false) final String talentParam,
            final HttpSession session,
            final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
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
     * 테스트 및 디버깅용 1,000 EXP 획득 치트를 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/cheat/exp")
    public String cheatExp(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final com.myapps.web.myrpg.application.dto.LevelUpResult result =
                progressionService.gainExperience(progress, 1000L);
        characterService.saveTurn(progress);

        if (result.levelsGained() > 0) {
            actionLog.add(
                    "테스트 치트: 1,000 EXP를 획득했습니다! (Lv."
                            + result.newLevel()
                            + " 달성, AP +"
                            + result.levelsGained()
                            + ")",
                    NOTIFICATION_TYPE);
        } else {
            actionLog.add("테스트 치트: 1,000 EXP를 획득했습니다!", NOTIFICATION_TYPE);
        }

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    /**
     * 테스트 및 디버깅용 1,000 Gold 획득 치트를 처리하고 갱신된 프래그먼트를 반환한다.
     *
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름 {@code "fragments/progress-response"}
     */
    @PostMapping("/cheat/gold")
    public String cheatGold(final HttpSession session, final Model model) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        progress.gainGold(1000);
        characterService.saveTurn(progress);
        actionLog.add("테스트 치트: 1,000 Gold를 획득했습니다!", NOTIFICATION_TYPE);

        final PlayScreenView view = buildViewFromProgress(progress);
        model.addAttribute("view", view);
        return "fragments/progress-response";
    }

    private CharacterProgress resolveCurrentCharacter(final HttpSession session) {
        if (session != null) {
            final Object sessionUser =
                    session.getAttribute(
                            com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor
                                    .SESSION_USER_KEY);
            if (sessionUser instanceof com.myapps.web.myrpg.application.dto.UserSession userSession
                    && userSession.characterId() != null) {
                return characterService.loadByCharacterId(userSession.characterId());
            }
        }
        return characterService.loadOrCreateDefault();
    }

    /**
     * 캐릭터 진행상황으로부터 플레이 화면 전체 뷰 모델을 조립한다.
     *
     * <p>현재 노드 기준 상호작용·상황 멘트·정보 팝업 조립은 {@link NodeViewAssembler}에 위임하여 전투 종료 후 화면 복원과 동일한 뷰를 공유한다.
     *
     * @param progress 캐릭터 진행상황
     * @return 플레이 화면 뷰 모델
     */
    private PlayScreenView buildViewFromProgress(final CharacterProgress progress) {
        return nodeViewAssembler.fromProgress(progress);
    }
}
