package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.StatLine;
import com.myapps.web.myrpg.application.dto.TopBarView;
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
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link PlayScreenController}의 환생·정보 팝업 관련 웹 슬라이스 테스트.
 *
 * <p>GET / 요청의 정보 팝업 상/중/하 렌더링(재능 라벨, StatLine, 환생 버튼 상태), 최대레벨 시 EXP "MAX" 표기, POST /rebirth
 * 가능/쿨다운 분기를 검증한다.
 */
@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerProgressionTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private MapService mapService;

    @MockitoBean private MovementService movementService;

    @MockitoBean private NpcService npcService;

    @MockitoBean private NpcDialogueService npcDialogueService;

    @MockitoBean private ProgressionService progressionService;

    @MockitoBean private ActionLog actionLog;

    @MockitoBean private PlayScreenViewHelper playScreenViewHelper;

    @MockitoBean private MonsterService monsterService;

    @MockitoBean private MonsterDialogueService monsterDialogueService;

    @MockitoBean private MonsterEncounterService monsterEncounterService;

    @MockitoBean private BattleService battleService;

    @MockitoBean private com.myapps.web.myrpg.application.service.DungeonService dungeonService;

    @MockitoBean private com.myapps.web.myrpg.domain.service.MapViewFactory mapViewFactory;

    @MockitoBean private com.myapps.web.myrpg.application.service.GatheringService gatheringService;

    @org.junit.jupiter.api.BeforeEach
    void setUpBattleServiceDefault() {
        org.mockito.Mockito.when(
                        battleService.resumeIfActive(
                                org.mockito.ArgumentMatchers.any(CharacterProgress.class)))
                .thenReturn(java.util.Optional.empty());
    }

    /**
     * GET / 정보 팝업 상/중/하 영역이 올바르게 렌더링되는지 검증한다. 재능 라벨, StatLine(STR/DEX/INT/CRIT/DEF 본체+보너스), 환생 버튼
     * 활성, 경과 텍스트를 확인한다.
     */
    @Test
    void should_renderInfoPopupTopMiddleBottom_when_rootAccessed() throws Exception {
        final PlayScreenView view = buildViewWithInfo(true, "환생 후 3시간 15분 경과");
        stubCommonForGet(view);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("근접전투")))
                .andExpect(content().string(containsString("Lv. 50")))
                .andExpect(content().string(containsString("STR")))
                .andExpect(content().string(containsString("23")))
                .andExpect(content().string(containsString("(+0)")))
                .andExpect(content().string(containsString("DEX")))
                .andExpect(content().string(containsString("INT")))
                .andExpect(content().string(containsString("CRIT")))
                .andExpect(content().string(containsString("5.0%")))
                .andExpect(content().string(containsString("DEF")))
                .andExpect(content().string(containsString("환생 후 3시간 15분 경과")));
    }

    /** GET / 정보 팝업에서 환생 버튼이 비활성화되는지 검증한다. */
    @Test
    void should_renderRebirthButtonDisabled_when_rebirthNotAvailable() throws Exception {
        final PlayScreenView view = buildViewWithInfo(false, "환생 기록 없음");
        stubCommonForGet(view);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("disabled")));
    }

    /** GET / 정보 팝업에서 환생 버튼이 활성화되는지 검증한다. */
    @Test
    void should_renderRebirthButtonEnabled_when_rebirthAvailable() throws Exception {
        final PlayScreenView view = buildViewWithInfo(true, "환생 후 25시간 0분 경과");
        stubCommonForGet(view);

        final String html =
                mockMvc.perform(get("/"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // 환생 버튼에 disabled 속성이 없어야 한다.
        // 템플릿: th:attr="disabled=${view.info.rebirthAvailable} ? null : 'disabled'"
        // rebirthAvailable=true이면 disabled 속성이 렌더링되지 않음
        // 버튼 태그 자체에 disabled가 포함되지 않는지 확인
        // (주의: "disabled"는 다른 맥락에서 나올 수 있으므로 rebirth-btn 근처를 확인)
        org.assertj.core.api.Assertions.assertThat(html)
                .contains("rebirth-btn")
                .doesNotContain("disabled=\"disabled\"");
    }

    /** GET / 상단바에서 최대레벨(100)일 때 EXP 게이지에 "MAX"가 표시되는지 검증한다. */
    @Test
    void should_renderExpMax_when_levelIs100() throws Exception {
        final GaugeView expMax = new GaugeView(0, 0, 100, "MAX");
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final TopBarView topBar = new TopBarView("고니", 100, expMax, gauge, gauge, gauge);
        final InfoPopupView info = buildInfo(true, "환생 후 25시간 0분 경과");
        final PlayScreenView view =
                new PlayScreenView(
                        topBar,
                        dummyMinimap(),
                        dummyFullMap(),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        info);
        stubCommonForGet(view);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MAX")));
    }

    /** POST /rebirth 환생 성공 시 saveTurn이 호출되고 "환생했습니다" 로그가 추가되는지 검증한다. */
    @Test
    void should_saveTurnAndLogRebirth_when_rebirthSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.rebirth(any(CharacterProgress.class), any(TalentType.class)))
                .thenReturn(new RebirthResult.Reborn());
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/rebirth"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        verify(characterService).saveTurn(progress);
        verify(actionLog).add("환생했습니다 (재능: 근접전투)", "system");
    }

    /** POST /rebirth 쿨다운 활성 시 saveTurn이 호출되지 않고 남은 시간 안내 로그가 추가되는지 검증한다. */
    @Test
    void should_notSaveTurnAndLogCooldown_when_rebirthCooldownActive() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();
        final Duration remaining = Duration.ofHours(5).plusMinutes(30);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.rebirth(any(CharacterProgress.class), any(TalentType.class)))
                .thenReturn(new RebirthResult.CooldownActive(remaining));
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/rebirth"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        verify(characterService, never()).saveTurn(any());
        verify(actionLog).add("환생까지 5시간 30분 남았습니다", "system");
    }

    /**
     * GET / 정보 팝업에 보유 AP 값과 재능 효과 요약이 올바르게 렌더링되는지 검증한다.
     *
     * <p>info-popup.html의 "보유 AP" 행에 AP 숫자가, "재능 효과" 행에 효과 요약 문자열이 노출된다.
     */
    @Test
    void should_renderAbilityPointsAndTalentEffectSummary_when_rootAccessed() throws Exception {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final GaugeView exp = new GaugeView(500, 250000, 0, "500 / 250000");
        final TopBarView topBar = new TopBarView("고니", 10, exp, gauge, gauge, gauge);
        final InfoPopupView info =
                new InfoPopupView(
                        "고니",
                        10,
                        12,
                        "활",
                        5,
                        "원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv",
                        gauge,
                        gauge,
                        gauge,
                        List.of(new StatLine("STR", "10", "+0")),
                        true,
                        "환생 후 25시간 0분 경과");
        final PlayScreenView view =
                new PlayScreenView(
                        topBar,
                        dummyMinimap(),
                        dummyFullMap(),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        info);
        stubCommonForGet(view);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("보유 AP")))
                .andExpect(content().string(containsString(">5<")))
                .andExpect(content().string(containsString("재능 효과")))
                .andExpect(
                        content().string(containsString("원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv")));
    }

    /** POST /rebirth?talent=ARCHERY 요청 시 ARCHERY 재능으로 환생이 수행되고 응답 로그에 "활" 재능이 반영되는지 검증한다. */
    @Test
    void should_rebirthWithArcheryTalent_when_talentParamIsArchery() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.rebirth(any(CharacterProgress.class), any(TalentType.class)))
                .thenReturn(new RebirthResult.Reborn());
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/rebirth").param("talent", "ARCHERY"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        verify(progressionService).rebirth(progress, TalentType.ARCHERY);
        verify(characterService).saveTurn(progress);
        verify(actionLog).add("환생했습니다 (재능: 활)", "system");
    }

    /** POST /rebirth (talent 파라미터 누락) 시 기본 재능 MELEE로 폴백되어 환생이 수행되는지 검증한다. */
    @Test
    void should_fallbackToMelee_when_talentParamMissing() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.rebirth(any(CharacterProgress.class), any(TalentType.class)))
                .thenReturn(new RebirthResult.Reborn());
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/rebirth"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"));

        verify(progressionService).rebirth(progress, TalentType.MELEE);
        verify(actionLog).add("환생했습니다 (재능: 근접전투)", "system");
    }

    /** POST /rebirth?talent=ARCHERY 쿨다운 활성 시 상태가 변경되지 않고 saveTurn이 호출되지 않음을 검증한다. */
    @Test
    void should_keepStateUnchanged_when_rebirthWithTalentDuringCooldown() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();
        final Duration remaining = Duration.ofHours(12).plusMinutes(45);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.rebirth(any(CharacterProgress.class), any(TalentType.class)))
                .thenReturn(new RebirthResult.CooldownActive(remaining));
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/rebirth").param("talent", "ARCHERY"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        verify(progressionService).rebirth(progress, TalentType.ARCHERY);
        verify(characterService, never()).saveTurn(any());
        verify(actionLog).add("환생까지 12시간 45분 남았습니다", "system");
    }

    /** POST /cheat/exp 요청 시 1,000 EXP가 지급되고 saveTurn 및 progress-response 프래그먼트가 반환되는지 검증한다. */
    @Test
    void should_gainExpAndSaveTurn_when_cheatExpRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(progressionService.gainExperience(progress, 1000L))
                .thenReturn(new com.myapps.web.myrpg.application.dto.LevelUpResult(0, 1));
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/cheat/exp"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        verify(progressionService).gainExperience(progress, 1000L);
        verify(characterService).saveTurn(progress);
        verify(actionLog).add("테스트 치트: 1,000 EXP를 획득했습니다!", "system");
    }

    /** POST /cheat/gold 요청 시 1,000 Gold가 지급되고 saveTurn 및 progress-response 프래그먼트가 반환되는지 검증한다. */
    @Test
    void should_gainGoldAndSaveTurn_when_cheatGoldRequested() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = buildDefaultView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        stubBuildViewFromProgress(view);

        mockMvc.perform(post("/cheat/gold"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/progress-response"))
                .andExpect(model().attributeExists("view"));

        assertThat(progress.getGold()).isEqualTo(1000);
        verify(characterService).saveTurn(progress);
        verify(actionLog).add("테스트 치트: 1,000 Gold를 획득했습니다!", "system");
    }

    // ─────────────────────────────────────── 헬퍼 메서드 ───────────────────────────────────────

    private void stubCommonForGet(final PlayScreenView view) {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final RebirthStatus rebirthStatus = new RebirthStatus(true, false, null, null);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(dummyNode());
        when(mapService.minimap(anyString())).thenReturn(dummyMinimap());
        when(mapService.fullMap(anyString())).thenReturn(dummyFullMap());
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(rebirthStatus);
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(view.info());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), anyList(), isNull(), isNull(), any(), any()))
                .thenReturn(view);
    }

    private void stubBuildViewFromProgress(final PlayScreenView view) {
        final RebirthStatus rebirthStatus = new RebirthStatus(true, false, null, null);

        when(mapService.node(anyString())).thenReturn(dummyNode());
        when(mapService.minimap(anyString())).thenReturn(dummyMinimap());
        when(mapService.fullMap(anyString())).thenReturn(dummyFullMap());
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(rebirthStatus);
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(view.info());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), anyList(), isNull(), isNull(), any(), any()))
                .thenReturn(view);
    }

    private PlayScreenView buildViewWithInfo(
            final boolean rebirthAvailable, final String elapsedText) {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final GaugeView exp = new GaugeView(500, 250000, 0, "500 / 250000");
        final TopBarView topBar = new TopBarView("고니", 50, exp, gauge, gauge, gauge);
        final InfoPopupView info = buildInfo(rebirthAvailable, elapsedText);
        return new PlayScreenView(
                topBar, dummyMinimap(), dummyFullMap(), null, null, null, null, List.of(), info);
    }

    private PlayScreenView buildDefaultView() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final TopBarView topBar = new TopBarView("고니", 1, gauge, gauge, gauge, gauge);
        final InfoPopupView info = buildInfo(true, "환생 기록 없음");
        return new PlayScreenView(
                topBar, dummyMinimap(), dummyFullMap(), null, null, null, null, List.of(), info);
    }

    private InfoPopupView buildInfo(final boolean rebirthAvailable, final String elapsedText) {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final List<StatLine> stats =
                List.of(
                        new StatLine("STR", "23", "+0"),
                        new StatLine("DEX", "15", "+0"),
                        new StatLine("INT", "10", "+0"),
                        new StatLine("CRIT", "5.0%", "+0.0%"),
                        new StatLine("DEF", "8", "+0"));
        return new InfoPopupView(
                "고니",
                50,
                51,
                "근접전투",
                0,
                "근접 데미지 +10%, STR +2/Lv, HP +5/Lv",
                gauge,
                gauge,
                gauge,
                stats,
                rebirthAvailable,
                elapsedText);
    }

    private MapNode dummyNode() {
        return new MapNode("test-node", "테스트 노드", "village", null, 0, 0, null, null, List.of());
    }

    private MinimapView dummyMinimap() {
        return new MinimapView("테스트맵", List.of());
    }

    private FullMapView dummyFullMap() {
        return new FullMapView(List.of(), 5, 5);
    }
}
