package com.myapps.web.myrpg.interfaces.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link BattleController}의 웹 슬라이스 테스트.
 *
 * <p>{@code POST /battle/start}로 전투 시작 시 battle-view 프래그먼트 반환, {@code POST /battle/turn}으로 턴 진행 시
 * battle-response 프래그먼트 반환, {@code POST /battle/flee}로 도망 시 적절한 프래그먼트 반환, {@code GET
 * /battle/skills}로 전투 스킬 목록 프래그먼트 반환을 검증한다.
 */
@WebMvcTest(BattleController.class)
class BattleControllerTest {

    private static final String FRAGMENT_BATTLE_VIEW = "fragments/battle-view :: battle-view";
    private static final String FRAGMENT_BATTLE_RESPONSE =
            "fragments/battle-view :: battle-response";
    private static final String FRAGMENT_BATTLE_SKILLS = "fragments/battle-view :: battle-skills";
    private static final String MONSTER_ID = "raccoon";
    private static final String MONSTER_NAME = "너구리";
    private static final int MONSTER_LEVEL = 1;
    private static final int MONSTER_MAX_HP = 25;
    private static final String SKILL_ID = "smash";
    private static final long CHARACTER_ID = 1L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BattleService battleService;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private MonsterService monsterService;

    @MockitoBean private MapService mapService;

    @MockitoBean private PlayScreenViewHelper playScreenViewHelper;

    @MockitoBean private ActionLog actionLog;

    @MockitoBean private NodeViewAssembler nodeViewAssembler;

    @MockitoBean private ItemCatalogService itemCatalogService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(nodeViewAssembler.fromProgress(any(CharacterProgress.class)))
                .thenReturn(createRestoredView());
    }

    /** POST /battle/start 요청 시 전투 응답 프래그먼트가 200으로 반환되는지 검증한다. */
    @Test
    void should_returnBattleResponseFragment_when_startWithValidMonsterId() throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final Monster monster = createTestMonster();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.start(any(CharacterProgress.class), eq(MONSTER_ID), eq(false)))
                .thenReturn(state);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(battleService.combatSkills(any(CharacterProgress.class)))
                .thenReturn(createTestSkills());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(mapService.minimap(anyString())).thenReturn(createTestMinimap());
        when(actionLog.getEntries()).thenReturn(List.of());

        // when & then
        final org.springframework.test.web.servlet.MvcResult result =
                mockMvc.perform(post("/battle/start").param("monsterId", MONSTER_ID))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("battleView"))
                        .andExpect(model().attributeExists("skills"))
                        .andReturn();

        final BattleView battleView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        org.assertj.core.api.Assertions.assertThat(battleView.standby()).isTrue();
        org.assertj.core.api.Assertions.assertThat(battleView.fleeAvailable()).isTrue();

        verify(actionLog).add("⚔️ 너구리 조우!", "combat");
    }

    /** POST /battle/turn 요청 시 전투 응답 프래그먼트(top-bar + battle-view + action-log)가 반환되는지 검증한다. */
    @Test
    void should_returnBattleResponseFragment_when_turnWithValidSkillId() throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final Monster monster = createTestMonster();
        final BattleTurnResult turnResult = createOngoingTurnResult();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.takeTurn(
                        any(CharacterProgress.class), any(BattleState.class), eq(SKILL_ID)))
                .thenReturn(turnResult);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(battleService.combatSkills(any(CharacterProgress.class)))
                .thenReturn(createTestSkills());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(mapService.minimap(anyString())).thenReturn(createTestMinimap());
        when(actionLog.getEntries()).thenReturn(List.of());

        // when & then
        final org.springframework.test.web.servlet.MvcResult result =
                mockMvc.perform(post("/battle/turn").param("skillId", SKILL_ID))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("view"))
                        .andExpect(model().attributeExists("battleView"))
                        .andExpect(model().attributeExists("turnResult"))
                        .andReturn();

        final BattleView battleView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        org.assertj.core.api.Assertions.assertThat(battleView.standby()).isTrue();
    }

    /** POST /battle/turn에서 전투 종료(승리) 시 battle-response 프래그먼트와 battleEnded 속성이 반환되는지 검증한다. */
    @Test
    void should_restoreMonsterInteractionButton_when_turnResultsInVictory() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final BattleTurnResult victoryResult = createVictoryTurnResult();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.takeTurn(
                        any(CharacterProgress.class), any(BattleState.class), eq(SKILL_ID)))
                .thenReturn(victoryResult);
        when(nodeViewAssembler.fromProgress(any(CharacterProgress.class)))
                .thenReturn(createRestoredView());

        mockMvc.perform(post("/battle/turn").param("skillId", SKILL_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                .andExpect(model().attributeExists("view"))
                .andExpect(model().attributeExists("turnResult"))
                .andExpect(model().attribute("battleEnded", true))
                .andExpect(model().attribute("outcome", Outcome.WIN))
                .andExpect(content().string(containsString("data-monster-id=\"raccoon\"")));
    }

    /** POST /battle/turn에서 던전 보스 처치 시 dungeonClear 모델 속성이 반환되는지 검증한다. */
    @Test
    void should_includeDungeonClearModel_when_bossDefeatedWithDungeonClearResult()
            throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, "giant-spider", 100, false);
        final DungeonClearResult clearResult =
                new DungeonClearResult(
                        "alby", "알비 던전", 300, 500, List.of(new DroppedItem("mana-potion-50", 2)));
        final BattleTurnResult victoryResult =
                new BattleTurnResult(
                        SkillType.NORMAL,
                        50,
                        SkillType.NORMAL,
                        0,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        true,
                        Outcome.WIN,
                        null,
                        100,
                        List.of(),
                        List.of("거대거미을(를) 처치했습니다!"),
                        clearResult);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.takeTurn(
                        any(CharacterProgress.class), any(BattleState.class), eq(SKILL_ID)))
                .thenReturn(victoryResult);
        when(itemCatalogService.byId("mana-potion-50"))
                .thenReturn(
                        Optional.of(new PotionItem("mana-potion-50", "마나 포션 50", 0, 50, 0, 10)));
        when(nodeViewAssembler.fromProgress(any(CharacterProgress.class)))
                .thenReturn(createRestoredView());

        mockMvc.perform(post("/battle/turn").param("skillId", SKILL_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                .andExpect(model().attributeExists("dungeonClear"))
                .andExpect(model().attribute("battleEnded", true))
                .andExpect(model().attribute("outcome", Outcome.WIN))
                .andExpect(content().string(containsString("dungeonClearModal")));
    }

    /** POST /battle/flee 도망 성공 시 전투 종료 응답이 반환되는지 검증한다. */
    @Test
    void should_returnBattleEndResponse_when_fleeSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final BattleTurnResult fleeSuccessResult = createFleeSuccessResult();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.flee(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(fleeSuccessResult);
        when(nodeViewAssembler.fromProgress(any(CharacterProgress.class)))
                .thenReturn(createRestoredView());

        mockMvc.perform(post("/battle/flee"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                .andExpect(model().attribute("battleEnded", true))
                .andExpect(model().attribute("outcome", Outcome.FLED))
                .andExpect(content().string(containsString("data-monster-id=\"raccoon\"")));
    }

    /** POST /battle/flee 도망 실패 시 전투 계속 응답이 반환되는지 검증한다. */
    @Test
    void should_returnOngoingBattleResponse_when_fleeFails() throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final Monster monster = createTestMonster();
        final BattleTurnResult fleeFailResult = createFleeFailResult();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.flee(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(fleeFailResult);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(battleService.combatSkills(any(CharacterProgress.class)))
                .thenReturn(createTestSkills());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(mapService.minimap(anyString())).thenReturn(createTestMinimap());
        when(actionLog.getEntries()).thenReturn(List.of());

        // when & then
        final org.springframework.test.web.servlet.MvcResult result =
                mockMvc.perform(post("/battle/flee"))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("battleView"))
                        .andExpect(model().attributeExists("turnResult"))
                        .andReturn();

        final BattleView battleView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        org.assertj.core.api.Assertions.assertThat(battleView.standby()).isTrue();
    }

    /** GET /battle/skills 요청 시 battle-skills 서브프래그먼트가 반환되는지 검증한다. */
    @Test
    void should_returnBattleSkillsFragment_when_skillsRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final List<BattleSkillButton> skills = createTestSkills();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.combatSkills(any(CharacterProgress.class))).thenReturn(skills);

        mockMvc.perform(get("/battle/skills"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_BATTLE_SKILLS))
                .andExpect(model().attributeExists("skills"));
    }

    // ─── 009: turnLog 모델 속성 및 battle-log 렌더 검증 ─────────────────────

    /** POST /battle/start 응답의 모델에 turnLog(인트로 라인)이 포함되는지 검증한다. */
    @Test
    void should_containTurnLogWithIntroLine_when_battleStarted() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final Monster monster = createTestMonster();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.start(any(CharacterProgress.class), eq(MONSTER_ID), eq(false)))
                .thenReturn(state);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(battleService.combatSkills(any(CharacterProgress.class)))
                .thenReturn(createTestSkills());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(mapService.minimap(anyString())).thenReturn(createTestMinimap());
        when(actionLog.getEntries()).thenReturn(List.of());

        final String expectedIntro = MONSTER_NAME + " Lv." + MONSTER_LEVEL + " 출현!";

        mockMvc.perform(post("/battle/start").param("monsterId", MONSTER_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("turnLog"))
                .andExpect(model().attribute("turnLog", contains(expectedIntro)));
    }

    /** POST /battle/turn 응답의 모델에 turnLog(combatLines)이 포함되고 렌더된 HTML에 battle-log 섹션이 존재하는지 검증한다. */
    @Test
    void should_containTurnLogFromCombatLines_when_turnProcessed() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final Monster monster = createTestMonster();
        final BattleTurnResult turnResult = createOngoingTurnResult();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.takeTurn(
                        any(CharacterProgress.class), any(BattleState.class), eq(SKILL_ID)))
                .thenReturn(turnResult);
        when(monsterService.byId(MONSTER_ID)).thenReturn(Optional.of(monster));
        when(battleService.combatSkills(any(CharacterProgress.class)))
                .thenReturn(createTestSkills());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(mapService.minimap(anyString())).thenReturn(createTestMinimap());
        when(actionLog.getEntries()).thenReturn(List.of());

        mockMvc.perform(post("/battle/turn").param("skillId", SKILL_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("turnLog"))
                .andExpect(model().attribute("turnLog", hasSize(turnResult.combatLines().size())))
                .andExpect(content().string(containsString("battle-log")))
                .andExpect(content().string(containsString("battle-log-line")));
    }

    /** GET /battle/skills 응답에 turnLog 모델 속성이 없고 battle-log 섹션이 렌더되지 않는지 검증한다 (스킬 프래그먼트만 반환). */
    @Test
    void should_notContainBattleLog_when_skillsFragmentReturned() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final List<BattleSkillButton> skills = createTestSkills();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.combatSkills(any(CharacterProgress.class))).thenReturn(skills);

        mockMvc.perform(get("/battle/skills"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("turnLog"))
                .andExpect(content().string(not(containsString("battle-log-line"))));
    }

    /** GET /battle/skills 요청 시 궁극기 쿨다운 및 준비완료 상태가 포함된 스킬 버튼 목록이 모델에 전달되는지 검증한다. */
    @Test
    void should_includeUltimateSkillsWithCooldown_when_skillsRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final List<BattleSkillButton> skills =
                List.of(
                        new BattleSkillButton(
                                "smash", "스매시", SkillType.HEAVY, ResourceKind.STAMINA, 5, 0, false),
                        new BattleSkillButton(
                                "final_hit",
                                "파이널 히트",
                                SkillType.ULTIMATE,
                                ResourceKind.STAMINA,
                                15,
                                5,
                                false),
                        new BattleSkillButton(
                                "meteor_strike",
                                "메테오 스트라이크",
                                SkillType.ULTIMATE,
                                ResourceKind.MP,
                                30,
                                0,
                                true));

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.combatSkills(any(CharacterProgress.class))).thenReturn(skills);

        mockMvc.perform(get("/battle/skills"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("skills"))
                .andExpect(model().attribute("skills", hasSize(3)));
    }

    // ─── 테스트 데이터 생성 헬퍼 ────────────────────────────────────────────

    private Monster createTestMonster() {
        return new Monster(
                MONSTER_ID,
                MONSTER_NAME,
                MonsterType.NORMAL,
                MONSTER_LEVEL,
                MONSTER_MAX_HP,
                10,
                3,
                50,
                20L,
                new GoldDrop(5, 15),
                List.of(),
                List.of("끼익!", "너구리가 경계한다.", "날카로운 발톱을 세운다."));
    }

    private List<BattleSkillButton> createTestSkills() {
        return List.of(
                new BattleSkillButton("smash", "스매시", SkillType.HEAVY, ResourceKind.STAMINA, 5),
                new BattleSkillButton(
                        "defense", "디펜스", SkillType.DEFENSE, ResourceKind.STAMINA, 3));
    }

    private TopBarView createTestTopBar() {
        final GaugeView exp = new GaugeView(0, 100, 0, "0 / 100");
        final GaugeView hp = new GaugeView(100, 100, 100, "100 / 100");
        final GaugeView mp = new GaugeView(50, 50, 100, "50 / 50");
        final GaugeView stamina = new GaugeView(80, 80, 100, "80 / 80");
        return new TopBarView("테스트용사", 1, exp, hp, mp, stamina);
    }

    private MinimapView createTestMinimap() {
        return new MinimapView("티르코네일", List.of());
    }

    private PlayScreenView createRestoredView() {
        final List<InteractionItem> interactions =
                List.of(new InteractionItem(MONSTER_ID, MONSTER_NAME, false));
        return new PlayScreenView(
                createTestTopBar(),
                createTestMinimap(),
                null,
                "숲 속 공터",
                null,
                null,
                interactions,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null);
    }

    private BattleTurnResult createOngoingTurnResult() {
        return new BattleTurnResult(
                SkillType.HEAVY,
                12,
                SkillType.NORMAL,
                5,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                false,
                Outcome.NONE,
                null,
                0L,
                List.of(),
                List.of("스매시(강)로 너구리에게 12 피해", "너구리의 일반공격, 5 피해를 입음"));
    }

    private BattleTurnResult createVictoryTurnResult() {
        return new BattleTurnResult(
                SkillType.HEAVY,
                25,
                SkillType.NORMAL,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                true,
                Outcome.WIN,
                null,
                20L,
                List.of(),
                List.of("스매시(강)로 너구리에게 25 피해 (크리티컬!)", "너구리이(가) 쓰러졌습니다!"));
    }

    private BattleTurnResult createFleeSuccessResult() {
        return new BattleTurnResult(
                null,
                0,
                null,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                true,
                Outcome.FLED,
                null,
                0L,
                List.of(),
                List.of("도망쳤다!"));
    }

    private BattleTurnResult createFleeFailResult() {
        return new BattleTurnResult(
                null,
                0,
                SkillType.NORMAL,
                8,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                false,
                Outcome.NONE,
                null,
                0L,
                List.of(),
                List.of("도망 실패! 너구리에게 8 피해"));
    }
}
