package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Monster;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 전투 전용 엔드포인트 컨트롤러.
 *
 * <p>{@code POST /battle/start}로 전투를 시작하고, {@code POST /battle/turn}으로 턴을 진행하며, {@code POST
 * /battle/flee}로 도망을 시도하고, {@code GET /battle/skills}로 현재 착용 무기 기준의 전투 스킬 목록을 반환한다.
 *
 * <p>각 엔드포인트는 전투 전용 프래그먼트({@code battle-view.html})를 렌더하여 클라이언트의 {@code .center} 영역을 교체한다. 턴/도망 응답은
 * {@code top-bar} + {@code battle-view} + {@code action-log}를 함께 포함하여 플레이어 게이지와 활동 로그도 실시간 갱신된다.
 */
@Controller
@RequestMapping("/battle")
public class BattleController {

    private static final String COMBAT_TYPE = "combat";

    private final BattleService battleService;
    private final CharacterService characterService;
    private final MonsterService monsterService;
    private final MapService mapService;
    private final PlayScreenViewHelper playScreenViewHelper;
    private final ActionLog actionLog;
    private final NodeViewAssembler nodeViewAssembler;

    /**
     * BattleController를 생성한다.
     *
     * @param battleService 전투 오케스트레이션 서비스
     * @param characterService 캐릭터 저장/로드 서비스
     * @param monsterService 몬스터 카탈로그 조회 서비스
     * @param mapService 맵 데이터 서비스 (미니맵 렌더용)
     * @param playScreenViewHelper 뷰 모델 조립 헬퍼
     * @param actionLog 세션 보관 행동 로그
     * @param nodeViewAssembler 현재 노드 기준 플레이 화면 뷰 조립 컴포넌트
     */
    public BattleController(
            final BattleService battleService,
            final CharacterService characterService,
            final MonsterService monsterService,
            final MapService mapService,
            final PlayScreenViewHelper playScreenViewHelper,
            final ActionLog actionLog,
            final NodeViewAssembler nodeViewAssembler) {
        this.battleService = battleService;
        this.characterService = characterService;
        this.monsterService = monsterService;
        this.mapService = mapService;
        this.playScreenViewHelper = playScreenViewHelper;
        this.actionLog = actionLog;
        this.nodeViewAssembler = nodeViewAssembler;
    }

    /**
     * 전투를 시작하고 전투 응답 프래그먼트를 반환한다.
     *
     * <p>지정된 몬스터 ID로 전투를 시작하며, 몬스터가 미지이거나 전투 시작에 실패하면 전투를 시작하지 않고 일반 센터 프래그먼트를 반환한다. 성공 시 마주침 로그를
     * 기록하고 전투 응답 뷰(상단바·몬스터 HP 바·스킬 버튼·도망 버튼·미니맵·활동 로그)를 렌더한다.
     *
     * @param monsterId 전투 대상 몬스터 ID
     * @param model Spring MVC 모델
     * @return 전투 응답 프래그먼트 뷰 이름
     */
    @PostMapping("/start")
    public String start(@RequestParam final String monsterId, final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final BattleState state = battleService.start(progress, monsterId, false);

        if (state == null) {
            return returnCenterFragment(progress, model);
        }

        final Optional<Monster> monsterOpt = monsterService.byId(monsterId);
        if (monsterOpt.isEmpty()) {
            return returnCenterFragment(progress, model);
        }

        final Monster monster = monsterOpt.get();
        actionLog.add(monster.name() + "와(과) 마주쳤다.", COMBAT_TYPE);
        final BattleView battleView = buildBattleView(state, monster, progress);
        final List<String> turnLog = List.of(monster.name() + " Lv." + monster.level() + " 출현!");
        populateBattleModel(model, progress, battleView, turnLog);
        return "fragments/battle-view :: battle-response";
    }

    /**
     * 전투 턴을 진행하고 갱신된 프래그먼트들을 반환한다.
     *
     * <p>플레이어가 선택한 스킬로 한 턴을 진행한다. 활성 전투가 없으면 일반 플레이 화면을 반환한다. 턴 결과에 따라 전투
     * 응답(top-bar+battle-view+action-log 교체) 또는 전투 종료 응답(top-bar+center+action-log 복원)을 렌더한다.
     *
     * @param skillId 플레이어가 선택한 스킬 ID
     * @param model Spring MVC 모델
     * @return 전투 응답 프래그먼트 뷰 이름
     */
    @PostMapping("/turn")
    public String turn(@RequestParam final String skillId, final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final Optional<BattleState> stateOpt = battleService.resumeIfActive(progress);

        if (stateOpt.isEmpty()) {
            return returnCenterFragment(progress, model);
        }

        final BattleState state = stateOpt.get();
        final String monsterId = state.getMonsterId();
        final BattleTurnResult result = battleService.takeTurn(progress, state, skillId);

        if (result.resourceInsufficient()) {
            return buildOngoingBattleResponse(progress, state, model, result);
        }

        if (result.battleEnded()) {
            return buildBattleEndResponse(progress, monsterId, model, result);
        }

        return buildOngoingBattleResponse(progress, state, model, result);
    }

    /**
     * 도망을 시도하고 결과에 따른 프래그먼트를 반환한다.
     *
     * <p>50% 확률로 성공하며, 성공 시 일반 플레이 화면으로 복원하고 실패 시 전투를 계속한다. 도망 실패로 사망하면 사망 처리 후 복원한다.
     *
     * @param model Spring MVC 모델
     * @return 전투 응답 프래그먼트 뷰 이름
     */
    @PostMapping("/flee")
    public String flee(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final Optional<BattleState> stateOpt = battleService.resumeIfActive(progress);

        if (stateOpt.isEmpty()) {
            return returnCenterFragment(progress, model);
        }

        final BattleState state = stateOpt.get();
        final String monsterId = state.getMonsterId();
        final BattleTurnResult result = battleService.flee(progress, state);

        if (result.battleEnded()) {
            return buildBattleEndResponse(progress, monsterId, model, result);
        }

        return buildOngoingBattleResponse(progress, state, model, result);
    }

    /**
     * 현재 착용 무기 기준의 전투 스킬 목록 프래그먼트를 반환한다.
     *
     * <p>무기 교체 후 {@code #battleSkills} 영역만 실시간으로 갱신할 때 사용한다.
     *
     * @param model Spring MVC 모델
     * @return 전투 스킬 서브프래그먼트 뷰 이름
     */
    @GetMapping("/skills")
    public String skills(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        final List<BattleSkillButton> skills = battleService.combatSkills(progress);
        model.addAttribute("skills", skills);
        return "fragments/battle-view :: battle-skills";
    }

    // ─── Private: battle view building ──────────────────────────────────────

    private BattleView buildBattleView(
            final BattleState state, final Monster monster, final CharacterProgress progress) {
        final List<BattleSkillButton> skills = battleService.combatSkills(progress);
        return new BattleView(
                monster.name(),
                monster.level(),
                state.getMonsterCurrentHp(),
                monster.maxHp(),
                skills,
                true);
    }

    private void populateBattleModel(
            final Model model,
            final CharacterProgress progress,
            final BattleView battleView,
            final List<String> turnLog) {
        final TopBarView topBar = playScreenViewHelper.buildTopBar(progress);
        final MinimapView minimap = nodeViewAssembler.fromProgress(progress).minimap();
        final List<ActionLogEntry> logs = actionLog.getEntries();

        model.addAttribute("view", buildViewShell(topBar, minimap, logs));
        model.addAttribute("battleView", battleView);
        model.addAttribute("skills", battleView.skills());
        model.addAttribute("turnLog", turnLog);
    }

    // ─── Private: response construction ─────────────────────────────────────

    private String buildOngoingBattleResponse(
            final CharacterProgress progress,
            final BattleState state,
            final Model model,
            final BattleTurnResult result) {
        final Optional<Monster> monsterOpt = monsterService.byId(state.getMonsterId());
        if (monsterOpt.isEmpty()) {
            return returnCenterFragment(progress, model);
        }
        final Monster monster = monsterOpt.get();
        final BattleView battleView = buildBattleView(state, monster, progress);
        populateBattleModel(model, progress, battleView, result.combatLines());
        model.addAttribute("turnResult", result);
        model.addAttribute("battleMonsterName", monster.name());
        return "fragments/battle-view :: battle-response";
    }

    private String buildBattleEndResponse(
            final CharacterProgress progress,
            final String monsterId,
            final Model model,
            final BattleTurnResult result) {
        model.addAttribute("view", nodeViewAssembler.fromProgress(progress));
        model.addAttribute("turnResult", result);
        model.addAttribute("battleEnded", true);
        model.addAttribute("outcome", result.outcome());
        model.addAttribute("battleMonsterName", resolveMonsterName(monsterId));
        model.addAttribute("turnLog", result.combatLines());
        return "fragments/battle-view :: battle-response";
    }

    private String returnCenterFragment(final CharacterProgress progress, final Model model) {
        model.addAttribute("view", nodeViewAssembler.fromProgress(progress));
        return "fragments/center :: center";
    }

    // ─── Private: view shell ────────────────────────────────────────────────

    private String resolveMonsterName(final String monsterId) {
        return monsterService.byId(monsterId).map(Monster::name).orElse("몬스터");
    }

    private PlayScreenView buildViewShell(
            final TopBarView topBar, final MinimapView minimap, final List<ActionLogEntry> logs) {
        return new PlayScreenView(
                topBar, minimap, null, null, null, null, null, null, null, null, null, null, null,
                logs, null);
    }
}
