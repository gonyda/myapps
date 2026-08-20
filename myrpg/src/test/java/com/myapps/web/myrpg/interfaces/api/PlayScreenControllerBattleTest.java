package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.TopBarView;
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
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@link PlayScreenController}의 전투 관련 웹 슬라이스 테스트.
 *
 * <p>기습 발동 시 자동 전투 시작 및 {@code ambushMonsterName} 설정, 기습 미발동 시 신호 미포함, {@code GET /} 전투 재개 시 {@code
 * battleView}·{@code battleActive} 설정, 전투 중 {@code POST /move} 이동 거부를 검증한다.
 *
 * <p><b>Feature: 008-battle-system</b>
 *
 * <p><b>Validates: Requirements 1.6, 1.7, 17.1, 17.3, 19.4, 24.3</b>
 */
@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerBattleTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private MapService mapService;

    @MockitoBean private AmbienceService ambienceService;

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

    @BeforeEach
    void setUpDefaults() {
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
    }

    /**
     * POST /move 이동 성공 시 기습이 발동하면 {@code ambushMonsterName}이 모델에 설정되고 {@code battleService.start}가
     * {@code ambush=true}로 호출되는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.1, 17.3</b>
     */
    @Test
    void should_setAmbushSignalAndStartBattle_when_preemptiveStrikeTriggered() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode("dugald-north");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, null);
        final Monster raccoon = createRaccoon();
        final BattleState battleState = new BattleState(1L, "raccoon", 25, true);

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final PlayScreenView movedView = createDummyPlayScreenView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt()))
                .thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("두갈드 아일", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("위험한 들판");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of(raccoon));
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(interactions);
        when(monsterEncounterService.rollPreemptiveStrike(List.of(raccoon)))
                .thenReturn(Optional.of(raccoon));
        when(battleService.start(any(CharacterProgress.class), eq("raccoon"), eq(true)))
                .thenReturn(battleState);
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), anyString(), any(), isNull(), isNull(), any(), any()))
                .thenReturn(movedView);

        final MvcResult result =
                mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/move-response"))
                        .andExpect(model().attributeExists("view"))
                        .andExpect(model().attributeExists("ambushMonsterName"))
                        .andReturn();

        final String ambushName =
                (String) result.getModelAndView().getModel().get("ambushMonsterName");
        assertThat(ambushName).isEqualTo("너구리");

        verify(battleService).start(any(CharacterProgress.class), eq("raccoon"), eq(true));
    }

    /**
     * POST /move 이동 성공 시 기습이 발동하지 않으면 {@code ambushMonsterName}이 모델에 없고 {@code
     * battleService.start}가 호출되지 않는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.1, 17.3</b>
     */
    @Test
    void should_notSetAmbushSignal_when_preemptiveStrikeNotTriggered() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode("dugald-north");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, null);
        final Monster raccoon = createRaccoon();

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final PlayScreenView movedView = createDummyPlayScreenView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt()))
                .thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("두갈드 아일", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("위험한 들판");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of(raccoon));
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(interactions);
        when(monsterEncounterService.rollPreemptiveStrike(List.of(raccoon)))
                .thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), anyString(), any(), isNull(), isNull(), any(), any()))
                .thenReturn(movedView);

        mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"))
                .andExpect(model().attributeExists("view"))
                .andExpect(model().attributeDoesNotExist("ambushMonsterName"));

        verify(battleService, never()).start(any(), anyString(), eq(true));
    }

    /**
     * GET / 요청 시 활성 전투가 존재하면 모델에 {@code battleView}와 {@code battleActive=true}가 포함되어 전투가 복원되는지
     * 검증한다.
     *
     * <p><b>Validates: Requirements 1.6</b>
     */
    @Test
    void should_restoreBattleView_when_activeBattleExistsOnPageLoad() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Monster raccoon = createRaccoon();
        final BattleState battleState = new BattleState(1L, "raccoon", 15, false);

        final List<BattleSkillButton> skills =
                List.of(
                        new BattleSkillButton(
                                "smash", "스매시", SkillType.HEAVY, ResourceKind.STAMINA, 5));

        final PlayScreenView playView = createDummyPlayScreenView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(battleState));
        when(monsterService.byId("raccoon")).thenReturn(Optional.of(raccoon));
        when(battleService.combatSkills(any(CharacterProgress.class))).thenReturn(skills);
        when(mapService.node(anyString())).thenReturn(dummyNode("tir-chonaill"));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        anyList(),
                        isNull(),
                        isNull(),
                        any(),
                        any()))
                .thenReturn(playView);

        final MvcResult result =
                mockMvc.perform(get("/"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("play"))
                        .andExpect(model().attributeExists("view"))
                        .andExpect(model().attributeExists("battleView"))
                        .andExpect(model().attribute("battleActive", true))
                        .andReturn();

        final BattleView actualBattleView =
                (BattleView) result.getModelAndView().getModel().get("battleView");
        assertThat(actualBattleView.monsterName()).isEqualTo("너구리");
        assertThat(actualBattleView.monsterLevel()).isEqualTo(1);
        assertThat(actualBattleView.monsterCurrentHp()).isEqualTo(15);
        assertThat(actualBattleView.monsterMaxHp()).isEqualTo(25);
        assertThat(actualBattleView.skills()).hasSize(1);
        assertThat(actualBattleView.fleeAvailable()).isTrue();
    }

    /**
     * GET / 요청 시 저장된 monsterId가 카탈로그에서 소실되었으면 전투가 안전 종료되어 {@code battleView}가 모델에 없는지 검증한다.
     *
     * <p><b>Validates: Requirements 1.7</b>
     */
    @Test
    void should_safeTerminateBattle_when_monsterIdMissingFromCatalog() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState battleState = new BattleState(1L, "deleted-monster", 10, false);

        final PlayScreenView playView = createDummyPlayScreenView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(battleState));
        when(monsterService.byId("deleted-monster")).thenReturn(Optional.empty());
        when(mapService.node(anyString())).thenReturn(dummyNode("tir-chonaill"));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        anyList(),
                        isNull(),
                        isNull(),
                        any(),
                        any()))
                .thenReturn(playView);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("play"))
                .andExpect(model().attributeExists("view"))
                .andExpect(model().attributeDoesNotExist("battleView"))
                .andExpect(model().attributeDoesNotExist("battleActive"));
    }

    /**
     * POST /move 요청 시 활성 전투가 존재하면 이동이 거부되고 실제 이동(movementService.move)이 호출되지 않는지 검증한다.
     *
     * <p><b>Validates: Requirements 19.4</b>
     */
    @Test
    void should_rejectMove_when_activeBattleExists() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final BattleState battleState = new BattleState(1L, "raccoon", 20, false);

        final PlayScreenView playView = createDummyPlayScreenView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.of(battleState));
        when(mapService.node(anyString())).thenReturn(dummyNode("tir-chonaill"));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        anyList(),
                        isNull(),
                        isNull(),
                        any(),
                        any()))
                .thenReturn(playView);

        mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"))
                .andExpect(model().attributeExists("view"));

        verify(movementService, never()).move(any(), anyInt(), anyInt());
        verify(characterService, never()).saveTurn(any());
        verify(actionLog).add("전투 중에는 이동할 수 없습니다.", "system");
    }

    // ─── 헬퍼 메서드 ───────────────────────────────────────────────────────

    private Monster createRaccoon() {
        return new Monster(
                "raccoon",
                "너구리",
                MonsterType.NORMAL,
                1,
                25,
                4,
                1,
                10,
                15L,
                new GoldDrop(3, 10),
                List.of(),
                List.of("크르릉… 쉭, 쉭!", "(너구리가 몸을 잔뜩 웅크린 채 경계 태세를 갖춘다.)"));
    }

    private MapNode dummyNode(final String nodeId) {
        return new MapNode(nodeId, "테스트 노드", "village", null, 0, 0, null, null, List.of());
    }

    private PlayScreenView createDummyPlayScreenView() {
        return new PlayScreenView(
                dummyTopBar(),
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                "평화로운 마을",
                null,
                null,
                List.of(),
                null,
                List.of(),
                dummyInfo());
    }

    private InfoPopupView dummyInfo() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new InfoPopupView(
                "고니",
                1,
                1,
                "근접전투",
                0,
                "근접 데미지 +10%, STR +2/Lv, HP +5/Lv",
                gauge,
                gauge,
                gauge,
                List.of(),
                true,
                "환생 기록 없음");
    }

    private TopBarView dummyTopBar() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new TopBarView("고니", 1, gauge, gauge, gauge, gauge);
    }
}
