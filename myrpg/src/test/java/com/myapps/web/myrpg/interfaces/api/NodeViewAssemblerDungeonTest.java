package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.TalkTarget;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MonsterService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.service.MapViewFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link NodeViewAssembler}의 던전 뷰 및 상호작용 버튼 조립 단위 테스트.
 *
 * <p>Requirements: 1.3, 4.1, 4.2, 5.1
 */
@ExtendWith(MockitoExtension.class)
class NodeViewAssemblerDungeonTest {

    private static final long CHARACTER_ID = 1L;

    @Mock private MapService mapService;
    @Mock private AmbienceService ambienceService;
    @Mock private NpcService npcService;
    @Mock private MonsterService monsterService;
    @Mock private ProgressionService progressionService;
    @Mock private PlayScreenViewHelper playScreenViewHelper;
    @Mock private DungeonService dungeonService;
    @Mock private MapViewFactory mapViewFactory;

    private ActionLog actionLog;
    private NodeViewAssembler nodeViewAssembler;
    private CharacterProgress character;

    @BeforeEach
    void setUp() {
        final Clock clock =
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        actionLog = new ActionLog(clock);

        nodeViewAssembler =
                new NodeViewAssembler(
                        mapService,
                        ambienceService,
                        npcService,
                        monsterService,
                        progressionService,
                        playScreenViewHelper,
                        actionLog,
                        dungeonService,
                        mapViewFactory);

        character = CharacterProgress.createDefault();
        setId(character, CHARACTER_ID);

        final RebirthStatus rebirthStatus = mock(RebirthStatus.class);
        final InfoPopupView info = mock(InfoPopupView.class);

        given(progressionService.rebirthStatus(any())).willReturn(rebirthStatus);
        given(playScreenViewHelper.buildInfo(any(), any())).willReturn(info);

        // Stub buildPlayScreen to return a populated view
        org.mockito.Mockito.lenient()
                .when(
                        playScreenViewHelper.buildPlayScreen(
                                any(), any(), any(), any(), any(), isNull(), isNull(), any(),
                                any()))
                .thenAnswer(
                        invocation -> {
                            final MinimapView mm = invocation.getArgument(1);
                            final FullMapView fm = invocation.getArgument(2);
                            final String amb = invocation.getArgument(3);
                            final List<InteractionItem> interactions = invocation.getArgument(4);
                            return new PlayScreenView(
                                    createTestTopBar(),
                                    mm,
                                    fm,
                                    amb,
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
                                    info);
                        });

        org.mockito.Mockito.lenient()
                .when(
                        playScreenViewHelper.buildPlayScreen(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(TalkTarget.class),
                                any(),
                                any()))
                .thenAnswer(
                        invocation -> {
                            final MinimapView mm = invocation.getArgument(1);
                            final FullMapView fm = invocation.getArgument(2);
                            final String amb = invocation.getArgument(3);
                            final List<InteractionItem> interactions = invocation.getArgument(4);
                            final TalkTarget tt = invocation.getArgument(5);
                            final String monsterName =
                                    tt != null && tt.monster() != null ? tt.monster().name() : null;
                            final String monsterDialogue = tt != null ? tt.dialogue() : null;
                            return new PlayScreenView(
                                    createTestTopBar(),
                                    mm,
                                    fm,
                                    amb,
                                    null,
                                    null,
                                    interactions,
                                    null,
                                    monsterName,
                                    monsterDialogue,
                                    null,
                                    null,
                                    null,
                                    List.of(),
                                    info);
                        });
    }

    @Test
    @DisplayName("던전 시작방(room-0-0)에서는 던전 맵과 함께 '던전 나가기' 상호작용 버튼이 제공된다")
    void should_assembleDungeonStartRoom_with_leaveButton() {
        // given
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-0-0", "room-2-0");
        final MinimapView minimap = new MinimapView("알비 던전", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 3, 3);

        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));
        given(mapViewFactory.createMinimap(dungeon)).willReturn(minimap);
        given(mapViewFactory.createFullMap(dungeon)).willReturn(fullMap);

        // when
        final PlayScreenView view = nodeViewAssembler.fromProgress(character);

        // then
        assertThat(view).isNotNull();
        assertThat(view.ambience()).isEqualTo("던전의 입구로 이어지는 안전한 시작방이다.");
        assertThat(view.minimap()).isEqualTo(minimap);
        assertThat(view.fullMap()).isEqualTo(fullMap);
        assertThat(view.interactions()).hasSize(1);
        assertThat(view.interactions().get(0).actionType()).isEqualTo("dungeon-leave");
        assertThat(view.interactions().get(0).name()).contains("던전 나가기");
    }

    @Test
    @DisplayName("던전 몬스터 방(room-1-0)에서는 해당 방의 잔여 몬스터 전투 버튼이 제공된다")
    void should_assembleMonsterRoom_with_monsterButtons() {
        // given
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-1-0", "room-2-0");
        final Monster spider =
                new Monster(
                        "spider",
                        "거미",
                        MonsterType.NORMAL,
                        2,
                        65,
                        48,
                        4,
                        30,
                        30L,
                        new GoldDrop(8, 20),
                        List.of(),
                        List.of("1", "2", "3"),
                        40,
                        30);

        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));
        given(mapViewFactory.createMinimap(dungeon))
                .willReturn(new MinimapView("알비 던전", List.of()));
        given(mapViewFactory.createFullMap(dungeon)).willReturn(new FullMapView(List.of(), 3, 3));
        given(monsterService.byId("spider")).willReturn(Optional.of(spider));

        // when
        final PlayScreenView view = nodeViewAssembler.fromProgress(character);

        // then
        assertThat(view).isNotNull();
        assertThat(view.ambience()).isEqualTo("어둡고 축축한 거미줄이 드리워진 던전 방이다.");
        assertThat(view.interactions()).hasSize(1);
        assertThat(view.interactions().get(0).actionType()).isEqualTo("monster");
        assertThat(view.interactions().get(0).id()).isEqualTo("spider");
    }

    @Test
    @DisplayName("던전 보스방(room-2-0)에서는 보스 전용 상황 멘트와 보스 전투 버튼이 제공된다")
    void should_assembleBossRoom_with_bossAmbienceAndMonster() {
        // given
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-2-0", "room-2-0");
        final Monster giantSpider =
                new Monster(
                        "giant-spider",
                        "거대거미",
                        MonsterType.BOSS,
                        7,
                        380,
                        72,
                        12,
                        70,
                        350L,
                        new GoldDrop(150, 300),
                        List.of(),
                        List.of("1", "2", "3"),
                        60,
                        50);

        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));
        given(mapViewFactory.createMinimap(dungeon))
                .willReturn(new MinimapView("알비 던전", List.of()));
        given(mapViewFactory.createFullMap(dungeon)).willReturn(new FullMapView(List.of(), 3, 3));
        given(monsterService.byId("giant-spider")).willReturn(Optional.of(giantSpider));

        // when
        final PlayScreenView view = nodeViewAssembler.fromProgress(character);

        // then
        assertThat(view).isNotNull();
        assertThat(view.ambience()).isEqualTo("거대한 거미줄이 사방을 뒤덮고 있으며 압도적인 위압감이 감돈다.");
        assertThat(view.interactions()).hasSize(1);
        assertThat(view.interactions().get(0).actionType()).isEqualTo("monster");
        assertThat(view.interactions().get(0).name()).contains("거대거미");
    }

    @Test
    @DisplayName("필드의 던전 입구 노드(alby-entrance)에서는 '알비 던전 입장' 상호작용 버튼이 제공된다")
    void should_assembleFieldDungeonEntrance_with_enterButton() {
        // given
        character.updateCurrentNodeId("alby-entrance");
        final MapNode entranceNode =
                new MapNode(
                        "alby-entrance",
                        "알비 던전 입구",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        -1,
                        "alby",
                        null,
                        List.of("tir-chonaill"),
                        List.of());

        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.empty());
        given(mapService.node("alby-entrance")).willReturn(entranceNode);
        given(mapService.minimap("alby-entrance"))
                .willReturn(new MinimapView("알비 던전 입구", List.of()));
        given(mapService.fullMap("alby-entrance")).willReturn(new FullMapView(List.of(), 5, 5));
        given(ambienceService.ambience(entranceNode)).willReturn("던전 입구다.");
        given(npcService.byNode("alby-entrance")).willReturn(List.of());
        given(monsterService.byNode("alby-entrance")).willReturn(List.of());
        given(playScreenViewHelper.buildInteractions(List.of(), List.of())).willReturn(List.of());

        // when
        final PlayScreenView view = nodeViewAssembler.fromProgress(character);

        // then
        assertThat(view).isNotNull();
        assertThat(view.interactions()).hasSize(1);
        assertThat(view.interactions().get(0).actionType()).isEqualTo("dungeon-enter");
        assertThat(view.interactions().get(0).name()).contains("알비 던전 입장");
    }

    @Test
    @DisplayName("던전 방에서 몬스터 조우 시 talkTarget 정보(대사, 몬스터명 등)가 뷰에 정상 반영된다")
    void should_assembleMonsterRoom_with_talkTarget_when_encounteringDungeonMonster() {
        // given
        final DungeonInstance dungeon = createDungeonInstance(CHARACTER_ID, "room-1-0", "room-2-0");
        final Monster spider =
                new Monster(
                        "spider",
                        "거미",
                        MonsterType.NORMAL,
                        2,
                        65,
                        48,
                        4,
                        30,
                        30L,
                        new GoldDrop(8, 20),
                        List.of(),
                        List.of("1", "2", "3"),
                        40,
                        30);

        final TalkTarget talkTarget = TalkTarget.ofMonster(spider, "샤아악-!");

        given(dungeonService.getActiveDungeon(CHARACTER_ID)).willReturn(Optional.of(dungeon));
        given(mapViewFactory.createMinimap(dungeon))
                .willReturn(new MinimapView("알비 던전", List.of()));
        given(mapViewFactory.createFullMap(dungeon)).willReturn(new FullMapView(List.of(), 3, 3));
        given(monsterService.byId("spider")).willReturn(Optional.of(spider));

        // when
        final PlayScreenView view = nodeViewAssembler.fromProgress(character, talkTarget);

        // then
        assertThat(view).isNotNull();
        assertThat(view.monsterName()).isEqualTo("거미");
        assertThat(view.monsterDialogue()).isEqualTo("샤아악-!");
        assertThat(view.interactions()).hasSize(1);
        assertThat(view.interactions().get(0).actionType()).isEqualTo("monster");
    }

    private TopBarView createTestTopBar() {
        final GaugeView exp = new GaugeView(0, 100, 0, "0 / 100");
        final GaugeView hp = new GaugeView(100, 100, 100, "100 / 100");
        final GaugeView mp = new GaugeView(50, 50, 100, "50 / 50");
        final GaugeView stamina = new GaugeView(80, 80, 100, "80 / 80");
        return new TopBarView("테스트용사", 1, exp, hp, mp, stamina);
    }

    private DungeonInstance createDungeonInstance(
            final Long charId, final String currentRoomId, final String bossRoomId) {
        final MapNode startNode =
                new MapNode(
                        "room-0-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of());
        final MapNode room1 =
                new MapNode(
                        "room-1-0",
                        "던전 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-0-0", "room-2-0"),
                        List.of("spider"));
        final MapNode bossRoom =
                new MapNode(
                        "room-2-0",
                        "거대거미의 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        2,
                        0,
                        "alby",
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of("giant-spider"));

        final MapGraph graph =
                new MapGraph(List.of(startNode, room1, bossRoom), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState(
                                        "room-2-0", false, false, List.of("giant-spider")));

        return new DungeonInstance(
                charId,
                "alby",
                "alby-entrance",
                "room-0-0",
                bossRoomId,
                currentRoomId,
                graph,
                roomStates);
    }

    private void setId(final CharacterProgress progress, final long id) {
        try {
            final java.lang.reflect.Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
