package com.myapps.web.myrpg.interfaces.api;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.ActionButton;
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
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link PlayScreenController}의 NPC 관련 웹 슬라이스 테스트.
 *
 * <p>NPC가 있는 노드에서의 상호작용 버튼 노출, NPC 대화 요청,
 * 노드 이동 시 상호작용 재구성, NPC가 없는 노드 시나리오를 검증한다.
 */
@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerNpcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterService characterService;

    @MockitoBean
    private MapService mapService;

    @MockitoBean
    private AmbienceService ambienceService;

    @MockitoBean
    private MovementService movementService;

    @MockitoBean
    private NpcService npcService;

    @MockitoBean
    private NpcDialogueService npcDialogueService;

    @MockitoBean
    private ProgressionService progressionService;

    @MockitoBean
    private ActionLog actionLog;

    @MockitoBean
    private PlayScreenViewHelper playScreenViewHelper;

    @MockitoBean
    private MonsterService monsterService;

    @MockitoBean
    private MonsterDialogueService monsterDialogueService;

    @MockitoBean
    private MonsterEncounterService monsterEncounterService;

    @MockitoBean
    private BattleService battleService;

    @org.junit.jupiter.api.BeforeEach
    void setUpBattleServiceDefault() {
        org.mockito.Mockito.when(battleService.resumeIfActive(
                org.mockito.ArgumentMatchers.any(CharacterProgress.class)))
                .thenReturn(java.util.Optional.empty());
    }

    /**
     * GET / 요청 시 NPC가 있는 노드에서 interactions에 NPC 버튼이 노출되고
     * npc-talk 영역(npcName/npcDialogue)이 비어 있으며 npcActions가 null인지 검증한다.
     */
    @Test
    void should_exposeNpcInteractionsAndEmptyTalk_when_nodeHasNpcs() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();
        final Npc neris = createNpc("neris", "네리스", NpcType.BLACKSMITH, nodeId);
        final Npc duncan = createNpc("duncan", "던컨", NpcType.CHIEF, nodeId);
        final List<Npc> npcs = List.of(neris, duncan);

        final List<InteractionItem> interactions = List.of(
                new InteractionItem("neris", "네리스 (대장간)", true),
                new InteractionItem("duncan", "던컨 (촌장)", true)
        );

        final PlayScreenView viewWithNpcs = new PlayScreenView(
                dummyTopBar(),
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                "평화로운 마을",
                null,
                null,
                interactions,
                null,
                List.of(),
                dummyInfo()
        );

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(nodeId)).thenReturn(npcs);
        when(monsterService.byNode(nodeId)).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(npcs, List.of())).thenReturn(interactions);
        when(monsterEncounterService.rollPreemptiveStrike(anyList())).thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                any(), any(), any(), anyString(), eq(interactions), isNull(), isNull(), any(), any()))
                .thenReturn(viewWithNpcs);

        final MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("play"))
                .andExpect(model().attributeExists("view"))
                .andReturn();

        final PlayScreenView actualView = (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.interactions()).hasSize(2);
        assertThat(actualView.interactions().get(0).name()).isEqualTo("네리스 (대장간)");
        assertThat(actualView.interactions().get(0).npc()).isTrue();
        assertThat(actualView.interactions().get(1).name()).isEqualTo("던컨 (촌장)");
        assertThat(actualView.interactions().get(1).npc()).isTrue();
        assertThat(actualView.npcName()).isNull();
        assertThat(actualView.npcDialogue()).isNull();
        assertThat(actualView.npcActions()).isNull();
    }

    /**
     * POST /npc/talk?npcId= 요청 시 npcName·대사·npcActions가 올바르게 노출되고
     * fragments/npc-response 뷰가 반환되는지 검증한다.
     * 이전 내용은 완전히 교체된다(이전 NPC 정보 미포함).
     */
    @Test
    void should_exposeNpcNameDialogueAndActions_when_talkToNpc() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();
        final Npc neris = createNpc("neris", "네리스", NpcType.BLACKSMITH, nodeId);
        final List<Npc> npcs = List.of(neris);

        final List<InteractionItem> interactions = List.of(
                new InteractionItem("neris", "네리스 (대장간)", true)
        );

        final List<ActionButton> npcActions = List.of(
                new ActionButton("상점"),
                new ActionButton("수리")
        );

        final PlayScreenView talkView = new PlayScreenView(
                dummyTopBar(),
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                "평화로운 마을",
                "네리스",
                "좋은 물건이 많아요!",
                interactions,
                npcActions,
                List.of(),
                dummyInfo()
        );

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(npcService.byNode(nodeId)).thenReturn(npcs);
        when(playScreenViewHelper.buildInteractions(npcs)).thenReturn(interactions);
        when(npcService.byId("neris")).thenReturn(Optional.of(neris));
        when(npcDialogueService.selectLine(neris)).thenReturn("좋은 물건이 많아요!");
        when(mapService.node(nodeId)).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(nodeId)).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(nodeId)).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(
                any(), any(), any(), anyString(), eq(interactions), eq(neris), eq("좋은 물건이 많아요!"), any()))
                .thenReturn(talkView);

        final MvcResult result = mockMvc.perform(post("/npc/talk").param("npcId", "neris"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/npc-response"))
                .andExpect(model().attributeExists("view"))
                .andReturn();

        final PlayScreenView actualView = (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.npcName()).isEqualTo("네리스");
        assertThat(actualView.npcDialogue()).isEqualTo("좋은 물건이 많아요!");
        assertThat(actualView.npcActions()).hasSize(2);
        assertThat(actualView.npcActions().get(0).label()).isEqualTo("상점");
        assertThat(actualView.npcActions().get(1).label()).isEqualTo("수리");
    }

    /**
     * POST /move로 다른 노드로 이동 시 interactions가 새 노드의 NPC로 재구성되는지 검증한다.
     */
    @Test
    void should_reconstructInteractions_when_moveToNodeWithDifferentNpcs() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode("dunbarton");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, null);

        final Npc aranwen = createNpc("aranwen", "아란웬", NpcType.SCHOOL, "dunbarton");
        final List<Npc> newNodeNpcs = List.of(aranwen);

        final List<InteractionItem> newInteractions = List.of(
                new InteractionItem("aranwen", "아란웬 (학교)", true)
        );

        final PlayScreenView movedView = new PlayScreenView(
                dummyTopBar(),
                new MinimapView("던바튼", List.of()),
                new FullMapView(List.of(), 5, 5),
                "활기찬 도시",
                null,
                null,
                newInteractions,
                null,
                List.of(),
                dummyInfo()
        );

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt())).thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("던바튼", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("활기찬 도시");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(anyString())).thenReturn(newNodeNpcs);
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(newNodeNpcs, List.of())).thenReturn(newInteractions);
        when(monsterEncounterService.rollPreemptiveStrike(anyList())).thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                any(), any(), any(), anyString(), eq(newInteractions), isNull(), isNull(), any(), any()))
                .thenReturn(movedView);

        final MvcResult result = mockMvc.perform(post("/move")
                        .param("dx", "1")
                        .param("dy", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"))
                .andExpect(model().attributeExists("view"))
                .andReturn();

        final PlayScreenView actualView = (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.interactions()).hasSize(1);
        assertThat(actualView.interactions().get(0).name()).isEqualTo("아란웬 (학교)");
        assertThat(actualView.interactions().get(0).npc()).isTrue();
        assertThat(actualView.npcName()).isNull();
        assertThat(actualView.npcDialogue()).isNull();
        assertThat(actualView.npcActions()).isNull();
    }

    /**
     * GET / 요청 시 NPC가 없는 노드에서 interactions가 빈 목록인지 검증한다.
     */
    @Test
    void should_returnEmptyInteractions_when_nodeHasNoNpcs() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();

        final PlayScreenView emptyView = new PlayScreenView(
                dummyTopBar(),
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                "평화로운 마을",
                null,
                null,
                List.of(),
                null,
                List.of(),
                dummyInfo()
        );

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(npcService.byNode(nodeId)).thenReturn(List.of());
        when(monsterService.byNode(nodeId)).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(List.of(), List.of())).thenReturn(List.of());
        when(monsterEncounterService.rollPreemptiveStrike(anyList())).thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                any(), any(), any(), anyString(), eq(List.of()), isNull(), isNull(), any(), any()))
                .thenReturn(emptyView);

        final MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("play"))
                .andExpect(model().attributeExists("view"))
                .andReturn();

        final PlayScreenView actualView = (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.interactions()).isEmpty();
        assertThat(actualView.npcName()).isNull();
        assertThat(actualView.npcDialogue()).isNull();
        assertThat(actualView.npcActions()).isNull();
    }

    private Npc createNpc(final String id, final String name, final NpcType type, final String nodeId) {
        final NpcLines lines = new NpcLines(List.of("안녕하세요."), null);
        return new Npc(id, name, type, nodeId, "친절한 성격", lines);
    }

    private MapNode dummyNode(final String nodeId) {
        return new MapNode(nodeId, "테스트 노드", "village", null, 0, 0, null, null, List.of());
    }

    private InfoPopupView dummyInfo() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new InfoPopupView("고니", 1, 1, "근접전투", 0, "근접 데미지 +10%, STR +2/Lv, HP +5/Lv", gauge, gauge, gauge, List.of(), true, "환생 기록 없음");
    }

    private TopBarView dummyTopBar() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new TopBarView("고니", 1, gauge, gauge, gauge, gauge);
    }
}
