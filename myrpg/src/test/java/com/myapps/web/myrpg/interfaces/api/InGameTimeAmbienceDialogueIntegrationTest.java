package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.BattleService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.application.service.GatheringService;
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
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.service.MapViewFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 캐릭터의 인게임 시간({@link CharacterProgress#getInGameHour()})에 따른 상황 멘트({@link AmbienceService}) 및 NPC
 * 대사({@link NpcDialogueService}) 연동 통합 테스트.
 */
@WebMvcTest(controllers = PlayScreenController.class)
@Import(NodeViewAssembler.class)
class InGameTimeAmbienceDialogueIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NodeViewAssembler nodeViewAssembler;

    @MockitoBean private AmbienceService ambienceService;
    @MockitoBean private NpcDialogueService npcDialogueService;
    @MockitoBean private CharacterService characterService;
    @MockitoBean private MapService mapService;
    @MockitoBean private NpcService npcService;
    @MockitoBean private MonsterService monsterService;
    @MockitoBean private MonsterDialogueService monsterDialogueService;
    @MockitoBean private MonsterEncounterService monsterEncounterService;
    @MockitoBean private BattleService battleService;
    @MockitoBean private MovementService movementService;
    @MockitoBean private ProgressionService progressionService;
    @MockitoBean private DungeonService dungeonService;
    @MockitoBean private PlayScreenViewHelper playScreenViewHelper;
    @MockitoBean private ActionLog actionLog;
    @MockitoBean private MapViewFactory mapViewFactory;
    @MockitoBean private GatheringService gatheringService;

    private CharacterProgress progress;
    private MapNode townNode;
    private Npc duncan;

    @BeforeEach
    void setUp() {
        progress = CharacterProgress.createDefault();
        townNode =
                new MapNode("tir-chonaill", "티르코네일", "village", null, 0, 0, null, null, List.of());
        final NpcLines lines =
                new NpcLines(
                        List.of("어서 오게."),
                        Map.of(
                                "late-night", List.of("이 시간까지 깨어 있다니."),
                                "morning", List.of("아침 공기가 맑군.")));
        duncan = new Npc("duncan", "던컨", NpcType.CHIEF, "tir-chonaill", "마을 촌장", lines);

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
        when(mapService.node(anyString())).thenReturn(townNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(npcService.byNode(anyString())).thenReturn(List.of(duncan));
        when(npcService.byId("duncan")).thenReturn(Optional.of(duncan));
        when(monsterService.byNode(anyString())).thenReturn(List.of());
        when(progressionService.rebirthStatus(any(CharacterProgress.class)))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(), any())).thenReturn(dummyInfo());
    }

    private InfoPopupView dummyInfo() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new InfoPopupView(
                "고니",
                1,
                1,
                "근접전투",
                0,
                "근접 데미지 +10%",
                gauge,
                gauge,
                gauge,
                List.of(),
                true,
                "환생 기록 없음");
    }

    @Test
    @DisplayName("NodeViewAssembler는 캐릭터의 인게임 시간(inGameMinutes)을 기반으로 AmbienceService를 호출한다")
    void should_passInGameHourToAmbienceService_when_assemblingFieldView() {
        // given: 02:00 심야 (inGameMinutes = 120, hour = 2)
        progress.setInGameMinutes(120);
        when(ambienceService.ambience(eq(townNode), eq(2))).thenReturn("심야의 고요한 마을");

        // when
        nodeViewAssembler.fromProgress(progress);

        // then
        final ArgumentCaptor<Integer> hourCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(ambienceService).ambience(eq(townNode), hourCaptor.capture());
        assertThat(hourCaptor.getValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("인게임 시간이 바뀌면(아침 08:00 vs 밤 20:00) 각각 해당 시간대의 hour가 전달된다")
    void should_passDifferentHour_when_inGameTimeChanges() {
        // given: 아침 08:00 (inGameMinutes = 480, hour = 8)
        progress.setInGameMinutes(480);
        when(ambienceService.ambience(eq(townNode), eq(8))).thenReturn("상쾌한 아침 마을");
        nodeViewAssembler.fromProgress(progress);

        // given: 밤 20:00 (inGameMinutes = 1200, hour = 20)
        progress.setInGameMinutes(1200);
        when(ambienceService.ambience(eq(townNode), eq(20))).thenReturn("어두운 밤 마을");
        nodeViewAssembler.fromProgress(progress);

        // then
        verify(ambienceService).ambience(eq(townNode), eq(8));
        verify(ambienceService).ambience(eq(townNode), eq(20));
    }

    @Test
    @DisplayName("NPC 대화 시(POST /npc/talk) 캐릭터의 인게임 시간(hour)을 기반으로 NpcDialogueService를 호출한다")
    void should_selectTimeSpecificNpcDialogue_basedOnInGameHour() throws Exception {
        // given: 심야 02:00 (inGameMinutes = 120, hour = 2)
        progress.setInGameMinutes(120);
        when(npcDialogueService.selectLine(eq(duncan), eq(2))).thenReturn("이 시간까지 깨어 있다니.");
        final PlayScreenView talkView =
                new PlayScreenView(
                        dummyTopBar(),
                        new MinimapView("테스트맵", List.of()),
                        new FullMapView(List.of(), 5, 5),
                        "심야의 고요한 마을",
                        "던컨",
                        "이 시간까지 깨어 있다니.",
                        List.of(),
                        List.of(),
                        List.of(),
                        dummyInfo());
        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), any(), any(), eq(duncan), eq("이 시간까지 깨어 있다니."), any()))
                .thenReturn(talkView);

        // when
        mockMvc.perform(post("/npc/talk").param("npcId", "duncan")).andExpect(status().isOk());

        // then
        final ArgumentCaptor<Integer> hourCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(npcDialogueService).selectLine(eq(duncan), hourCaptor.capture());
        assertThat(hourCaptor.getValue()).isEqualTo(2);
    }

    private com.myapps.web.myrpg.application.dto.TopBarView dummyTopBar() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        return new com.myapps.web.myrpg.application.dto.TopBarView(
                "고니", 1, gauge, gauge, gauge, gauge);
    }
}
