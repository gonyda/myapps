package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@link BattleController}의 {@code POST /battle/clash} 공방 개시 엔드포인트 전용 웹 슬라이스 테스트.
 *
 * <p>대치 페이즈에서 [⚔️ 공방 개시] 요청 시 공방 페이즈 전환({@code standby=false}), 몬스터 전조 뱃지 라벨/CSS 클래스, 실시간 타이머 바
 * 지속시간(ms), 활 선제 사격 플래그 및 활성 전투 부재 시 센터 프래그먼트 복귀를 검증한다.
 *
 * <p><b>Feature: 013-active-telegraph-combat</b>
 */
@WebMvcTest(BattleController.class)
class BattleControllerClashTest {

    private static final String FRAGMENT_BATTLE_RESPONSE =
            "fragments/battle-view :: battle-response";
    private static final String FRAGMENT_CENTER = "fragments/center :: center";
    private static final String MONSTER_ID = "goblin";
    private static final String MONSTER_NAME = "고블린";
    private static final int MONSTER_LEVEL = 3;
    private static final int MONSTER_MAX_HP = 40;
    private static final long CHARACTER_ID = 1L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BattleService battleService;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private MonsterService monsterService;

    @MockitoBean private MapService mapService;

    @MockitoBean private PlayScreenViewHelper playScreenViewHelper;

    @MockitoBean private ActionLog actionLog;

    @MockitoBean private NodeViewAssembler nodeViewAssembler;

    @BeforeEach
    void setUp() {
        when(nodeViewAssembler.fromProgress(any(CharacterProgress.class)))
                .thenReturn(createRestoredView());
        when(playScreenViewHelper.buildTopBar(any(CharacterProgress.class)))
                .thenReturn(createTestTopBar());
        when(actionLog.getEntries()).thenReturn(List.of());
    }

    /** POST /battle/clash 요청 시 강공격 전조와 함께 공방 응답 프래그먼트가 반환되는지 검증한다. */
    @Test
    @DisplayName("활성 전투가 존재할 때 POST /battle/clash 호출 시 강공격 전조 및 1.5초 타이머 뷰를 반환한다")
    void should_returnBattleResponseWithHeavyClashView_when_activeBattleExists() throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final List<BattleSkillButton> skills = createTestSkills();
        final BattleView clashView =
                new BattleView(
                        MONSTER_NAME,
                        MONSTER_LEVEL,
                        MONSTER_MAX_HP,
                        MONSTER_MAX_HP,
                        skills,
                        false,
                        false,
                        SkillType.HEAVY,
                        1500,
                        "💥 강공격 차징 중!",
                        "badge-stance-heavy",
                        false);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.startClash(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(clashView);

        // when & then
        final MvcResult result =
                mockMvc.perform(post("/battle/clash"))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("view"))
                        .andExpect(model().attributeExists("battleView"))
                        .andExpect(model().attributeExists("skills"))
                        .andExpect(model().attributeExists("turnLog"))
                        .andReturn();

        final BattleView actualView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        assertThat(actualView.standby()).isFalse();
        assertThat(actualView.fleeAvailable()).isFalse();
        assertThat(actualView.monsterIntent()).isEqualTo(SkillType.HEAVY);
        assertThat(actualView.clashDurationMs()).isEqualTo(1500);
        assertThat(actualView.monsterStanceBadgeLabel()).isEqualTo("💥 강공격 차징 중!");
        assertThat(actualView.monsterStanceBadgeClass()).isEqualTo("badge-stance-heavy");
        assertThat(actualView.bowFirstStrike()).isFalse();

        verify(battleService).startClash(any(CharacterProgress.class), any(BattleState.class));
    }

    /** POST /battle/clash 요청 시 일반공격 전조(1.0초)가 정상 매핑되는지 검증한다. */
    @Test
    @DisplayName("몬스터 의도가 일반공격일 때 1.0초 타이머와 일반공격 뱃지 뷰를 반환한다")
    void should_returnBattleResponseWithNormalClashView_when_monsterIntentIsNormal()
            throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final List<BattleSkillButton> skills = createTestSkills();
        final BattleView clashView =
                new BattleView(
                        MONSTER_NAME,
                        MONSTER_LEVEL,
                        MONSTER_MAX_HP,
                        MONSTER_MAX_HP,
                        skills,
                        false,
                        false,
                        SkillType.NORMAL,
                        1000,
                        "⚡ 일반공격 태세",
                        "badge-stance-normal",
                        false);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.startClash(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(clashView);

        // when & then
        final MvcResult result =
                mockMvc.perform(post("/battle/clash"))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("battleView"))
                        .andReturn();

        final BattleView actualView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        assertThat(actualView.standby()).isFalse();
        assertThat(actualView.monsterIntent()).isEqualTo(SkillType.NORMAL);
        assertThat(actualView.clashDurationMs()).isEqualTo(1000);
        assertThat(actualView.monsterStanceBadgeLabel()).isEqualTo("⚡ 일반공격 태세");
        assertThat(actualView.monsterStanceBadgeClass()).isEqualTo("badge-stance-normal");
    }

    /** POST /battle/clash 요청 시 방어태세 전조(1.5초)가 정상 매핑되는지 검증한다. */
    @Test
    @DisplayName("몬스터 의도가 방어태세일 때 1.5초 타이머와 방어 뱃지 뷰를 반환한다")
    void should_returnBattleResponseWithDefenseClashView_when_monsterIntentIsDefense()
            throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final List<BattleSkillButton> skills = createTestSkills();
        final BattleView clashView =
                new BattleView(
                        MONSTER_NAME,
                        MONSTER_LEVEL,
                        MONSTER_MAX_HP,
                        MONSTER_MAX_HP,
                        skills,
                        false,
                        false,
                        SkillType.DEFENSE,
                        1500,
                        "🛡️ 방어 태세",
                        "badge-stance-defense",
                        false);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.startClash(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(clashView);

        // when & then
        final MvcResult result =
                mockMvc.perform(post("/battle/clash"))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("battleView"))
                        .andReturn();

        final BattleView actualView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        assertThat(actualView.standby()).isFalse();
        assertThat(actualView.monsterIntent()).isEqualTo(SkillType.DEFENSE);
        assertThat(actualView.clashDurationMs()).isEqualTo(1500);
        assertThat(actualView.monsterStanceBadgeLabel()).isEqualTo("🛡️ 방어 태세");
        assertThat(actualView.monsterStanceBadgeClass()).isEqualTo("badge-stance-defense");
    }

    /** POST /battle/clash 요청 시 활 1턴 선제 사격 상태가 정상 매핑되는지 검증한다. */
    @Test
    @DisplayName("활 1턴 선제 사격 시 전조 뱃지와 bowFirstStrike 플래그가 정상 매핑된다")
    void should_returnBattleResponseWithBowFirstStrike_when_bowEquippedOnFirstTurn()
            throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState state = new BattleState(CHARACTER_ID, MONSTER_ID, MONSTER_MAX_HP, false);
        final List<BattleSkillButton> skills = createTestSkills();
        final BattleView clashView =
                new BattleView(
                        MONSTER_NAME,
                        MONSTER_LEVEL,
                        MONSTER_MAX_HP,
                        MONSTER_MAX_HP,
                        skills,
                        false,
                        false,
                        null,
                        1500,
                        "🎯 선제 사격 기회!",
                        null,
                        true);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(state));
        when(battleService.startClash(any(CharacterProgress.class), any(BattleState.class)))
                .thenReturn(clashView);

        // when & then
        final MvcResult result =
                mockMvc.perform(post("/battle/clash"))
                        .andExpect(status().isOk())
                        .andExpect(view().name(FRAGMENT_BATTLE_RESPONSE))
                        .andExpect(model().attributeExists("battleView"))
                        .andReturn();

        final BattleView actualView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        assertThat(actualView.standby()).isFalse();
        assertThat(actualView.bowFirstStrike()).isTrue();
        assertThat(actualView.monsterIntent()).isNull();
        assertThat(actualView.monsterStanceBadgeLabel()).isEqualTo("🎯 선제 사격 기회!");
    }

    /** POST /battle/clash 요청 시 활성 전투가 없으면 일반 센터 프래그먼트를 반환하는지 검증한다. */
    @Test
    @DisplayName("활성 전투가 없을 때 POST /battle/clash 호출 시 센터 프래그먼트를 반환한다")
    void should_returnCenterFragment_when_clashRequestedWithNoActiveBattle() throws Exception {
        // given
        final CharacterProgress progress = CharacterProgress.createDefault();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/battle/clash"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_CENTER))
                .andExpect(model().attributeExists("view"));
    }

    // ─── 테스트 헬퍼 ────────────────────────────────────────────────────────

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
        return new MinimapView("던바튼", List.of());
    }

    private PlayScreenView createRestoredView() {
        final List<InteractionItem> interactions =
                List.of(new InteractionItem(MONSTER_ID, MONSTER_NAME, false));
        return new PlayScreenView(
                createTestTopBar(),
                createTestMinimap(),
                null,
                "던바튼 외곽",
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
}
