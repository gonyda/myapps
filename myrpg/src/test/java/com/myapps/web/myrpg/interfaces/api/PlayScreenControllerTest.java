package com.myapps.web.myrpg.interfaces.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.service.AmbienceService;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.MapService;
import com.myapps.web.myrpg.application.service.MovementService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * {@link PlayScreenController}의 웹 슬라이스 테스트.
 *
 * <p>GET / 요청 및 POST /move 요청에 대한 뷰 렌더링,
 * 이동 성공/거부 시 프래그먼트 반환 및 상태 저장을 검증한다.
 */
@WebMvcTest(PlayScreenController.class)
class PlayScreenControllerTest {

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
    private ActionLog actionLog;

    @MockitoBean
    private PlayScreenViewHelper playScreenViewHelper;

    /**
     * GET / 요청 시 play 뷰가 반환되는지 검증한다.
     */
    @Test
    void should_returnPlayView_when_rootAccessed() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final PlayScreenView view = createDummyView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(dummyNode());
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(any(), any(), any(), anyString(), any()))
                .thenReturn(view);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("play"))
                .andExpect(model().attributeExists("view"));
    }

    /**
     * POST /move 이동 성공 시 saveTurn이 호출되고 프래그먼트 뷰가 반환되는지 검증한다.
     */
    @Test
    void should_saveTurnAndReturnFragment_when_moveSucceeds() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MapNode targetNode = dummyNode();
        final ActionLogEntry logEntry = new ActionLogEntry("2025-01-01 12:00:00", "이동했습니다.", "move");
        final MovementResult.Moved moved = new MovementResult.Moved(targetNode, logEntry);
        final PlayScreenView view = createDummyView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt())).thenReturn(moved);
        when(characterService.saveTurn(any(CharacterProgress.class))).thenReturn(progress);
        when(mapService.node(anyString())).thenReturn(targetNode);
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("새로운 장소");
        when(actionLog.getEntries()).thenReturn(List.of(logEntry));
        when(playScreenViewHelper.buildPlayScreen(any(), any(), any(), anyString(), any()))
                .thenReturn(view);

        mockMvc.perform(post("/move")
                        .param("dx", "1")
                        .param("dy", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"))
                .andExpect(model().attributeExists("view"));

        verify(characterService).saveTurn(progress);
    }

    /**
     * POST /move 이동 거부 시 saveTurn이 호출되지 않고 안내 로그가 추가되는지 검증한다.
     */
    @Test
    void should_notSave_when_moveBlocked() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MovementResult.Blocked blocked = new MovementResult.Blocked("그곳으로는 갈 수 없습니다.");
        final PlayScreenView view = createDummyView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt())).thenReturn(blocked);
        when(mapService.node(anyString())).thenReturn(dummyNode());
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(any(), any(), any(), anyString(), any()))
                .thenReturn(view);

        mockMvc.perform(post("/move")
                        .param("dx", "0")
                        .param("dy", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"));

        verify(characterService, never()).saveTurn(any());
        verify(actionLog, never()).add(anyString(), anyString());
    }

    /**
     * POST /move 던전 진입 거부 시 로그 없이 무시되는지 검증한다.
     */
    @Test
    void should_notSaveAndNotLog_when_dungeonLocked() throws Exception {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final MovementResult.DungeonLocked locked = new MovementResult.DungeonLocked("아직 준비 중입니다.");
        final PlayScreenView view = createDummyView();

        when(characterService.loadOrCreateDefault()).thenReturn(progress);
        when(movementService.move(any(CharacterProgress.class), anyInt(), anyInt())).thenReturn(locked);
        when(mapService.node(anyString())).thenReturn(dummyNode());
        when(mapService.minimap(anyString())).thenReturn(new MinimapView("테스트맵", List.of()));
        when(mapService.fullMap(anyString())).thenReturn(new FullMapView(List.of(), 5, 5));
        when(ambienceService.ambience(any(MapNode.class))).thenReturn("평화로운 마을");
        when(actionLog.getEntries()).thenReturn(List.of());
        when(playScreenViewHelper.buildPlayScreen(any(), any(), any(), anyString(), any()))
                .thenReturn(view);

        mockMvc.perform(post("/move")
                        .param("dx", "0")
                        .param("dy", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/move-response"));

        verify(characterService, never()).saveTurn(any());
        verify(actionLog, never()).add(anyString(), anyString());
    }

    /**
     * POST /move에 파라미터가 없으면 400 응답을 반환하는지 검증한다.
     */
    @Test
    void should_returnBadRequest_when_moveParamsMissing() throws Exception {
        mockMvc.perform(post("/move"))
                .andExpect(status().isBadRequest());
    }

    private MapNode dummyNode() {
        return new MapNode("test-node", "테스트 노드", "village", null, 0, 0, null, null, List.of());
    }

    private PlayScreenView createDummyView() {
        final GaugeView gauge = new GaugeView(100, 100, 100, "100 / 100");
        final TopBarView topBar = new TopBarView("고니", 1, gauge, gauge, gauge, gauge);
        return new PlayScreenView(
                topBar,
                new MinimapView("테스트맵", List.of()),
                new FullMapView(List.of(), 5, 5),
                "평화로운 마을",
                null,
                null,
                null,
                List.of()
        );
    }
}
