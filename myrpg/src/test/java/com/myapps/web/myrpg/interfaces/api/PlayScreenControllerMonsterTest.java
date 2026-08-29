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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.ActionButton;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.TalkTarget;
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
 * {@link PlayScreenController}의 몬스터 조우 관련 웹 슬라이스 테스트.
 *
 * <p>몬스터 조우 엔드포인트({@code POST /monster/encounter}), 이동 후 몬스터 버튼 노출, 미지 몬스터 ID 관용 처리, NPC·몬스터 슬롯
 * 배타성을 검증한다.
 */
@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerMonsterTest {

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

    @org.junit.jupiter.api.BeforeEach
    void setUpBattleServiceDefault() {
        org.mockito.Mockito.when(
                        battleService.resumeIfActive(
                                org.mockito.ArgumentMatchers.any(CharacterProgress.class)))
                .thenReturn(java.util.Optional.empty());
    }

    /**
     * POST /monster/encounter 요청 시 몬스터가 존재하면 monster-response 뷰가 반환되고
     * monsterName/monsterLevel/monsterMaxHp/monsterDialogue/monsterActions가 채워지는지 검증한다.
     */
    @Test
    void should_returnMonsterResponseWithMonsterSlots_when_encounterKnownMonster()
            throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();
        final Monster raccoon = createRaccoon();

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final List<ActionButton> monsterActions = List.of(new ActionButton("전투"));

        final PlayScreenView monsterView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("테스트맵", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "어두운 숲",
                        null,
                        null,
                        interactions,
                        null,
                        "너구리",
                        "크르릉… 쉭, 쉭!",
                        1,
                        25,
                        monsterActions,
                        List.of(),
                        dummyInfo());

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(npcService.byNode(nodeId)).thenReturn(List.of());
        when(monsterService.byNode(nodeId)).thenReturn(List.of(raccoon));
        when(playScreenViewHelper.buildInteractions(List.of(), List.of(raccoon)))
                .thenReturn(interactions);
        when(monsterService.byId("raccoon")).thenReturn(Optional.of(raccoon));
        when(monsterDialogueService.selectLine(raccoon)).thenReturn("크르릉… 쉭, 쉭!");
        when(mapService.node(nodeId)).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(nodeId)).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(nodeId)).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("어두운 숲");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        eq(interactions),
                        any(TalkTarget.class),
                        any(),
                        isNull()))
                .thenReturn(monsterView);

        final MvcResult result =
                mockMvc.perform(post("/monster/encounter").param("monsterId", "raccoon"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/monster-response"))
                        .andExpect(model().attributeExists("view"))
                        .andReturn();

        final PlayScreenView actualView =
                (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.monsterName()).isEqualTo("너구리");
        assertThat(actualView.monsterDialogue()).isEqualTo("크르릉… 쉭, 쉭!");
        assertThat(actualView.monsterLevel()).isEqualTo(1);
        assertThat(actualView.monsterMaxHp()).isEqualTo(25);
        assertThat(actualView.monsterActions()).hasSize(1);
        assertThat(actualView.monsterActions().get(0).label()).isEqualTo("전투");

        verify(actionLog, never()).add(anyString(), anyString());
    }

    /**
     * POST /monster/encounter 요청 시 미지 monsterId이면 monster-response가 반환되고 몬스터 슬롯이 모두 null로 비워지는지
     * 검증한다(관용 처리).
     */
    @Test
    void should_returnEmptyMonsterSlots_when_encounterUnknownMonster() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();

        final List<InteractionItem> interactions = List.of();

        final PlayScreenView emptyMonsterView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("테스트맵", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "평화로운 마을",
                        null,
                        null,
                        interactions,
                        null,
                        List.of(),
                        dummyInfo());

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(npcService.byNode(nodeId)).thenReturn(List.of());
        when(monsterService.byNode(nodeId)).thenReturn(List.of());
        when(playScreenViewHelper.buildInteractions(List.of(), List.of())).thenReturn(interactions);
        when(monsterService.byId("unknown")).thenReturn(Optional.empty());
        when(mapService.node(nodeId)).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(nodeId)).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(nodeId)).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        eq(interactions),
                        any(TalkTarget.class),
                        any(),
                        isNull()))
                .thenReturn(emptyMonsterView);

        final MvcResult result =
                mockMvc.perform(post("/monster/encounter").param("monsterId", "unknown"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/monster-response"))
                        .andExpect(model().attributeExists("view"))
                        .andReturn();

        final PlayScreenView actualView =
                (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.monsterName()).isNull();
        assertThat(actualView.monsterDialogue()).isNull();
        assertThat(actualView.monsterLevel()).isNull();
        assertThat(actualView.monsterMaxHp()).isNull();
        assertThat(actualView.monsterActions()).isNull();
    }

    /** POST /monster/encounter 요청 시 NPC 슬롯이 null이고 몬스터 슬롯만 채워지는지 검증한다 (NPC·몬스터 슬롯 배타). */
    @Test
    void should_haveNullNpcSlots_when_monsterEncounter() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final String nodeId = progress.getCurrentNodeId();
        final Monster raccoon = createRaccoon();

        final List<InteractionItem> interactions =
                List.of(new InteractionItem("raccoon", "너구리", false));

        final List<ActionButton> monsterActions = List.of(new ActionButton("전투"));

        final PlayScreenView monsterView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("테스트맵", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "어두운 숲",
                        null,
                        null,
                        interactions,
                        null,
                        "너구리",
                        "크르릉… 쉭, 쉭!",
                        1,
                        25,
                        monsterActions,
                        List.of(),
                        dummyInfo());

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(npcService.byNode(nodeId)).thenReturn(List.of());
        when(monsterService.byNode(nodeId)).thenReturn(List.of(raccoon));
        when(playScreenViewHelper.buildInteractions(List.of(), List.of(raccoon)))
                .thenReturn(interactions);
        when(monsterService.byId("raccoon")).thenReturn(Optional.of(raccoon));
        when(monsterDialogueService.selectLine(raccoon)).thenReturn("크르릉… 쉭, 쉭!");
        when(mapService.node(nodeId)).thenReturn(dummyNode(nodeId));
        when(mapService.minimap(nodeId)).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(nodeId)).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class), anyInt())).thenReturn("어두운 숲");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        eq(interactions),
                        any(TalkTarget.class),
                        any(),
                        isNull()))
                .thenReturn(monsterView);

        final MvcResult result =
                mockMvc.perform(post("/monster/encounter").param("monsterId", "raccoon"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/monster-response"))
                        .andReturn();

        final PlayScreenView actualView =
                (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.npcName()).isNull();
        assertThat(actualView.npcDialogue()).isNull();
        assertThat(actualView.npcActions()).isNull();
        assertThat(actualView.monsterName()).isEqualTo("너구리");
    }

    /** POST /move로 몬스터가 있는 노드에 이동 후 interactions에 몬스터 버튼이 포함되는지 검증한다. */
    @Test
    void should_includeMonsterButtonInInteractions_when_moveToNodeWithMonsters() throws Exception {
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
        when(playScreenViewHelper.buildInteractions(List.of(), List.of(raccoon)))
                .thenReturn(interactions);
        when(monsterEncounterService.rollPreemptiveStrike(anyList())).thenReturn(Optional.empty());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(CharacterProgress.class), any(RebirthStatus.class)))
                .thenReturn(dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(),
                        any(),
                        any(),
                        anyString(),
                        eq(interactions),
                        isNull(),
                        isNull(),
                        any(),
                        any()))
                .thenReturn(movedView);

        final MvcResult result =
                mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("fragments/move-response"))
                        .andExpect(model().attributeExists("view"))
                        .andReturn();

        final PlayScreenView actualView =
                (PlayScreenView) result.getModelAndView().getModel().get("view");
        assertThat(actualView.interactions()).hasSize(1);
        assertThat(actualView.interactions().get(0).id()).isEqualTo("raccoon");
        assertThat(actualView.interactions().get(0).name()).isEqualTo("너구리");
        assertThat(actualView.interactions().get(0).npc()).isFalse();
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
