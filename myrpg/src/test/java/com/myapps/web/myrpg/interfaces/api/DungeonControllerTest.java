package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.application.exception.DungeonNotImplementedException;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link DungeonController}의 웹 계층 단위 테스트.
 *
 * <p>Requirements: 1.2, 1.4, 7.1
 */
@WebMvcTest(DungeonController.class)
class DungeonControllerTest {

    private static final String FRAGMENT_MOVE_RESPONSE = "fragments/move-response";
    private static final String DUNGEON_ID = "alby";
    private static final String TARGET_ROOM_ID = "room-1-0";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DungeonService dungeonService;

    @MockitoBean private CharacterService characterService;

    @MockitoBean private NodeViewAssembler nodeViewAssembler;

    private CharacterProgress character;
    private PlayScreenView mockView;

    @BeforeEach
    void setUp() {
        character = CharacterProgress.createDefault();
        mockView = createTestPlayScreenView();

        given(characterService.loadOrCreateDefault()).willReturn(character);
        given(nodeViewAssembler.fromProgress(any(CharacterProgress.class))).willReturn(mockView);
    }

    @Test
    @DisplayName("POST /dungeon/enter 요청 시 던전 입장 후 move-response 프래그먼트와 뷰 모델을 반환한다")
    void should_enterDungeon_and_returnMoveResponseFragment() throws Exception {
        mockMvc.perform(post("/dungeon/enter").param("dungeonId", DUNGEON_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_MOVE_RESPONSE))
                .andExpect(model().attributeExists("view"));

        then(dungeonService).should().enterDungeon(any(), eq(DUNGEON_ID));
        then(nodeViewAssembler).should().fromProgress(character);
    }

    @Test
    @DisplayName("POST /dungeon/leave 요청 시 던전 퇴장 후 move-response 프래그먼트와 뷰 모델을 반환한다")
    void should_leaveDungeon_and_returnMoveResponseFragment() throws Exception {
        mockMvc.perform(post("/dungeon/leave"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_MOVE_RESPONSE))
                .andExpect(model().attributeExists("view"));

        then(dungeonService).should().leaveDungeon(any());
        then(nodeViewAssembler).should().fromProgress(character);
    }

    @Test
    @DisplayName("POST /dungeon/move 요청 시 지정된 방으로 이동 후 move-response 프래그먼트를 반환한다")
    void should_moveToRoom_and_returnMoveResponseFragment() throws Exception {
        mockMvc.perform(post("/dungeon/move").param("targetRoomId", TARGET_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENT_MOVE_RESPONSE))
                .andExpect(model().attributeExists("view"));

        then(dungeonService).should().moveToRoom(any(), eq(TARGET_ROOM_ID));
        then(nodeViewAssembler).should().fromProgress(character);
    }

    @Test
    @DisplayName("미구현 던전 입장 시 400 Bad Request 에러 뷰를 반환한다")
    void should_returnBadRequest_when_unimplementedDungeon() throws Exception {
        doThrow(new DungeonNotImplementedException("ciar"))
                .when(dungeonService)
                .enterDungeon(any(), eq("ciar"));

        mockMvc.perform(post("/dungeon/enter").param("dungeonId", "ciar"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    @DisplayName("미클리어 방에서 차단된 전진 이동 시도 시 400 Bad Request 에러 뷰를 반환한다")
    void should_returnBadRequest_when_movementBlocked() throws Exception {
        doThrow(new BlockedMovementException("현재 방의 몬스터를 모두 처치해야 전진할 수 있습니다."))
                .when(dungeonService)
                .moveToRoom(any(), eq("room-2-0"));

        mockMvc.perform(post("/dungeon/move").param("targetRoomId", "room-2-0"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    private PlayScreenView createTestPlayScreenView() {
        final GaugeView exp = new GaugeView(0, 100, 0, "0 / 100");
        final GaugeView hp = new GaugeView(100, 100, 100, "100 / 100");
        final GaugeView mp = new GaugeView(50, 50, 100, "50 / 50");
        final GaugeView stamina = new GaugeView(80, 80, 100, "80 / 80");
        final TopBarView topBar = new TopBarView("테스트용사", 1, exp, hp, mp, stamina);
        final MinimapView minimap = new MinimapView("시작방", List.of());
        final FullMapView fullMap = new FullMapView(List.of(), 1, 1);

        return new PlayScreenView(
                topBar,
                minimap,
                fullMap,
                "던전 안입니다.",
                null,
                null,
                List.of(),
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
