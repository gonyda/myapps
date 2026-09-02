package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.myapps.web.myrpg.application.dto.StatLine;
import com.myapps.web.myrpg.application.dto.TopBarView;
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
import com.myapps.web.myrpg.domain.service.MapViewFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayScreenController.class)
@Import(NodeViewAssembler.class)
class PlayScreenControllerGatheringTest {

    private static final String FRAGMENT_MOVE_RESPONSE = "fragments/move-response";

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
    @MockitoBean private DungeonService dungeonService;
    @MockitoBean private MapViewFactory mapViewFactory;
    @MockitoBean private GatheringService gatheringService;

    @BeforeEach
    void setUp() {
        when(battleService.resumeIfActive(any(CharacterProgress.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("노드 이동 성공 시 GatheringService.rollTreeSpawn이 호출되고 나무 상호작용 버튼이 뷰에 포함된다")
    void should_rollTreeSpawn_and_includeGatherWoodButton_when_moved() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode =
                new MapNode(
                        "east-hill", "동쪽 언덕", "field", null, 1, 0, null, null, List.of("red-fox"));

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(movementService.move(progress, 1, 0))
                .thenAnswer(
                        invocation -> {
                            progress.updateCurrentNodeId("east-hill");
                            return new MovementResult.Moved(
                                    targetNode,
                                    new com.myapps.web.myrpg.domain.model.ActionLogEntry(
                                            "이동", "system", "12:00"));
                        });
        when(mapService.node("east-hill")).thenReturn(targetNode);
        when(mapService.minimap("east-hill")).thenReturn(new MinimapView("동쪽 언덕", List.of()));
        when(mapService.fullMap("east-hill")).thenReturn(new FullMapView(List.of(), 5, 5));
        when(actionLog.getEntries()).thenReturn(List.of());
        when(progressionService.rebirthStatus(progress))
                .thenReturn(new RebirthStatus(true, false, null, null));
        when(playScreenViewHelper.buildInfo(any(), any())).thenReturn(dummyInfo());
        when(playScreenViewHelper.buildInteractions(any(), any())).thenReturn(List.of());
        when(monsterService.byNode("east-hill")).thenReturn(List.of());
        when(monsterEncounterService.rollPreemptiveStrike(any())).thenReturn(Optional.empty());
        when(gatheringService.isTreeAvailable(any(), eq("east-hill"))).thenReturn(true);

        final PlayScreenView view =
                createDummyView(
                        List.of(
                                new InteractionItem(
                                        "gather-wood", "나무", false, "gathering", "wood")));

        when(playScreenViewHelper.buildPlayScreen(
                        any(), any(), any(), any(), isNull(), isNull(), any(), any()))
                .thenReturn(view);

        mockMvc.perform(post("/move").param("dx", "1").param("dy", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_MOVE_RESPONSE))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(org.hamcrest.Matchers.containsString("나무")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-action-type=\"gathering\"")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-target-param=\"wood\"")));

        verify(gatheringService).rollTreeSpawn(any(), eq("east-hill"), eq("field"));
    }

    private PlayScreenView createDummyView(final List<InteractionItem> interactions) {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final TopBarView topBar = new TopBarView("고니", 1, gauge, gauge, gauge, gauge);
        final InfoPopupView info = dummyInfo();
        return new PlayScreenView(
                topBar,
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                null,
                null,
                interactions,
                null,
                List.of(),
                info);
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
                List.of(new StatLine("STR", "5", "+0")),
                true,
                "환생 기록 없음");
    }
}
