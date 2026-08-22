package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * 던전 내부 진입 거부 프로퍼티 테스트.
 *
 * <p>임의의 던전 ID와 임의의 캐릭터 상태(다양한 currentNodeId)에 대해 {@code enterDungeon()}이 항상 {@link
 * MovementResult.DungeonLocked}를 반환하고 캐릭터의 현재 노드 ID가 변하지 않는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 10: 던전 내부 진입 거부
 *
 * <p><b>Validates: Requirements 6.3</b>
 */
// Feature: 001-character-progress-and-map-movement, Property 10: 던전 내부 진입 거부
class MovementServiceDungeonLockedPropertyTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    /**
     * 임의의 던전 ID와 임의의 캐릭터 상태에서 {@code enterDungeon()} 호출 시 항상 {@link MovementResult.DungeonLocked}를
     * 반환하고, 캐릭터의 {@code currentNodeId}가 호출 전과 동일한지 검증한다.
     *
     * @param scenario 임의 생성된 (currentNodeId, dungeonId) 2-튜플
     */
    @Property(tries = 100)
    void should_returnDungeonLocked_and_preserveCurrentNode_forAnyDungeonId(
            @ForAll("dungeonEntryScenario") final Tuple.Tuple2<String, String> scenario) {

        final String currentNodeId = scenario.get1();
        final String dungeonId = scenario.get2();

        final MapService mockMapService = mock(MapService.class);
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);
        final MovementService movementService =
                new MovementService(mockMapService, actionLog, null);

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
                        currentNodeId,
                        0,
                        0L);

        // When
        final MovementResult result = movementService.enterDungeon(progress, dungeonId);

        // Then: 결과가 DungeonLocked
        assertThat(result).isInstanceOf(MovementResult.DungeonLocked.class);

        final MovementResult.DungeonLocked locked = (MovementResult.DungeonLocked) result;

        // Then: 안내 메시지가 비어있지 않음
        assertThat(locked.message()).isNotBlank();

        // Then: 현재 노드 id 불변
        assertThat(progress.getCurrentNodeId()).isEqualTo(currentNodeId);
    }

    /**
     * 임의의 currentNodeId와 dungeonId 쌍을 생성하는 Arbitrary.
     *
     * <p>currentNodeId는 실제 맵 노드 ID 형태의 다양한 문자열, dungeonId는 임의의 영문+숫자+하이픈 조합 문자열을 생성한다.
     *
     * @return (currentNodeId, dungeonId) 2-튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<String, String>> dungeonEntryScenario() {
        final Arbitrary<String> nodeIds =
                Arbitraries.of(
                        "tir-chonaill",
                        "dunbarton",
                        "bangor",
                        "emain-macha",
                        "node-0",
                        "node-1",
                        "node-99",
                        "field-center",
                        "dungeon-entrance-1",
                        "town-square",
                        "forest-path");

        final Arbitrary<String> dungeonIds =
                Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .ofMinLength(1)
                        .ofMaxLength(30)
                        .map(s -> "dungeon-" + s);

        return nodeIds.flatMap(nodeId -> dungeonIds.map(dId -> Tuple.of(nodeId, dId)));
    }
}
