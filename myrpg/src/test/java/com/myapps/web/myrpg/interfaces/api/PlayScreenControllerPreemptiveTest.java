package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@link PlayScreenController}의 몬스터 기습(강제 전투 돌입) 판정 관련 웹 슬라이스 테스트.
 *
 * <p>{@code POST /move} 요청 시 기습 발동/미발동에 따른 {@code ambushMonsterName} 모델 속성 설정 및 전투 자동 시작을 검증한다.
 */
@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerPreemptiveTest {

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

    @MockitoBean private com.myapps.web.myrpg.application.service.DungeonService dungeonService;

    @MockitoBean private com.myapps.web.myrpg.domain.service.MapViewFactory mapViewFactory;

    @MockitoBean private com.myapps.web.myrpg.application.service.GatheringService gatheringService;

    @org.junit.jupiter.api.BeforeEach
    void setUpBattleServiceDefault() {
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(java.util.Optional.empty());
    }

    /** POST /move 이동 성공 시 기습이 발동하면 ambushMonsterName이 모델에 설정되고 battleService.start가 호출되는지 검증한다. */
    @Test
    void should_setAmbushMonsterNameAndStartBattle_when_preemptiveStrikeTriggered()
            throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode("dugald-north");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, null);
        final Monster raccoon = createRaccoon();

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final PlayScreenView movedView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("두갈드 아일", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "위험한 들판",
                        null,
                        null,
                        interactions,
                        null,
                        List.of(),
                        dummyInfo());

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt()))
                .thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("두갈드 아일", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("위험한 들판");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of(raccoon));
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(interactions);
        when(monsterEncounterService.rollPreemptiveStrike(List.of(raccoon)))
                .thenReturn(Optional.of(raccoon));
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

        verify(battleService)
                .start(
                        any(CharacterProgress.class),
                        org.mockito.ArgumentMatchers.eq("raccoon"),
                        org.mockito.ArgumentMatchers.eq(true));
        verify(actionLog).add("🚨 너구리 기습!", "combat");
    }

    /**
     * POST /move 이동 성공 시 기습이 발동하지 않으면 ambushMonsterName이 모델에 없고 battleService.start가 호출되지 않는지 검증한다.
     */
    @Test
    void should_notSetAmbushMonsterName_when_preemptiveStrikeNotTriggered() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode("dugald-north");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, null);
        final Monster raccoon = createRaccoon();

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final PlayScreenView movedView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("두갈드 아일", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "위험한 들판",
                        null,
                        null,
                        interactions,
                        null,
                        List.of(),
                        dummyInfo());

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt()))
                .thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("두갈드 아일", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("위험한 들판");
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

        final MvcResult result =
                mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/move-response"))
                        .andExpect(model().attributeExists("view"))
                        .andExpect(model().attributeDoesNotExist("ambushMonsterName"))
                        .andReturn();

        verify(battleService, never())
                .start(any(), anyString(), org.mockito.ArgumentMatchers.eq(true));
    }

    /** POST /move 이동이 거부(Blocked)되면 기습 판정이 수행되지 않는지 검증한다. */
    @Test
    void should_notRollPreemptive_when_moveBlocked() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MovementResult.Blocked blocked = new MovementResult.Blocked("그곳으로는 갈 수 없습니다.");

        final PlayScreenView blockedView =
                new PlayScreenView(
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

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt()))
                .thenReturn(blocked);
        when(mapService.node(anyString())).thenReturn(dummyNode("test-node"));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(List.of());
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(anyList(), anyList())).thenReturn(List.of());
        when(monsterEncounterService.rollPreemptiveStrike(anyList())).thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), anyString(), any(), isNull(), isNull(), any(), any()))
                .thenReturn(blockedView);

        mockMvc.perform(post("/move").param("dx", "0").param("dy", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"))
                .andExpect(model().attributeDoesNotExist("ambushMonsterName"));

        verify(monsterEncounterService, never()).rollPreemptiveStrike(anyList());
        verify(battleService, never())
                .start(any(), anyString(), org.mockito.ArgumentMatchers.eq(true));
    }

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
                List.of(
                        "크르릉… 쉭, 쉭!",
                        "(너구리가 몸을 잔뜩 웅크린 채 경계 태세를 갖춘다.)",
                        "(너구리가 이빨을 드러내며 앞발을 천천히 들어올린다.)"));
    }

    private MapNode dummyNode(final String nodeId) {
        return new MapNode(nodeId, "테스트 노드", "village", null, 0, 0, null, null, List.of());
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
