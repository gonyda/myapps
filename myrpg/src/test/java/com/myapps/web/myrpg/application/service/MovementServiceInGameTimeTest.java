package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link MovementService}의 이동 성공 및 차단(Blocked) 시 인게임 시간 경과 규칙을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class MovementServiceInGameTimeTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private MapService mapService;
    @Mock private DungeonService dungeonService;

    private ActionLog actionLog;
    private MovementService movementService;

    @BeforeEach
    void setUp() {
        actionLog = new ActionLog(FIXED_CLOCK);
        movementService = new MovementService(mapService, actionLog, dungeonService);
    }

    @Test
    @DisplayName("월드 맵 노드 이동 성공 시 인게임 시간이 정확히 15분 경과한다")
    void should_advanceTimeBy15Minutes_when_worldMoveSucceeds() {
        // given
        final MapNode townNode =
                new MapNode(
                        "tir-chonaill",
                        "티르코네일",
                        "town",
                        NodeType.TOWN,
                        0,
                        0,
                        null,
                        null,
                        List.of("dugald-north"));
        final MapNode fieldNode =
                new MapNode(
                        "dugald-north",
                        "두갈드 아일 북부",
                        "field",
                        NodeType.FIELD,
                        0,
                        1,
                        null,
                        null,
                        List.of("tir-chonaill"));
        final MapGraph graph =
                new MapGraph(List.of(townNode, fieldNode), List.of(), "tir-chonaill");

        when(mapService.node("tir-chonaill")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);
        progress.setInGameMinutes(480); // 08:00
        assertThat(progress.getInGameTimeFormatted()).isEqualTo("08:00");

        // when (남쪽 이동: dx=0, dy=1)
        final MovementResult result = movementService.move(progress, 0, 1);

        // then
        assertThat(result).isInstanceOf(MovementResult.Moved.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("dugald-north");
        assertThat(progress.getInGameMinutes()).isEqualTo(495); // 08:15
        assertThat(progress.getInGameTimeFormatted()).isEqualTo("08:15");
    }

    @Test
    @DisplayName("이동 불가(Blocked: 노드가 없거나 연결되지 않음) 방향 클릭 시 인게임 시간은 변하지 않는다")
    void should_notAdvanceTime_when_moveIsBlocked() {
        // given (두갈드 아일에서 좌/우 이동 시도 시 막힘 시나리오)
        final MapNode isleNode =
                new MapNode(
                        "dugald-isle",
                        "두갈드 아일",
                        "field",
                        NodeType.FIELD,
                        0,
                        2,
                        null,
                        null,
                        List.of("dugald-north", "dugald-south"));
        final MapGraph graph = new MapGraph(List.of(isleNode), List.of(), "dugald-isle");

        when(mapService.node("dugald-isle")).thenReturn(isleNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "dugald-isle",
                        0,
                        0L);
        progress.setInGameMinutes(480); // 08:00

        // when (동쪽 이동: dx=1, dy=0 -> 해당 방향에 노드 없음)
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("dugald-isle");
        assertThat(progress.getInGameMinutes()).isEqualTo(480); // 시간 불변 (08:00)
        assertThat(progress.getInGameTimeFormatted()).isEqualTo("08:00");
    }
}
