package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link DungeonRoomState} 불변 레코드 단위 테스트. */
class DungeonRoomStateTest {

    @Test
    @DisplayName("null remainingMonsters가 전달되면 빈 불변 리스트로 대체된다")
    void should_defaultToEmptyList_when_remainingMonstersIsNull() {
        // given
        final String roomId = "room-0-0";

        // when
        final DungeonRoomState state = new DungeonRoomState(roomId, false, false, null);

        // then
        assertThat(state.roomId()).isEqualTo(roomId);
        assertThat(state.cleared()).isFalse();
        assertThat(state.discovered()).isFalse();
        assertThat(state.remainingMonsters()).isEmpty();
    }

    @Test
    @DisplayName("외부 리스트가 변경되어도 방어적 복사로 인해 내부 상태가 보호된다")
    void should_defensivelyCopy_when_externalListIsModified() {
        // given
        final List<String> mutableMonsters = new ArrayList<>();
        mutableMonsters.add("spider");
        final DungeonRoomState state =
                new DungeonRoomState("room-0-0", false, true, mutableMonsters);

        // when
        mutableMonsters.add("red-spider");

        // then
        assertThat(state.remainingMonsters()).containsExactly("spider");
        assertThatThrownBy(() -> state.remainingMonsters().add("goblin"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("withDiscovered 호출 시 발견 상태만 변경된 새 객체가 반환된다")
    void should_returnNewInstanceWithUpdatedDiscovered_when_withDiscoveredCalled() {
        // given
        final DungeonRoomState original =
                new DungeonRoomState("room-0-0", false, false, List.of("spider"));

        // when
        final DungeonRoomState updated = original.withDiscovered(true);

        // then
        assertThat(updated.discovered()).isTrue();
        assertThat(updated.cleared()).isFalse();
        assertThat(updated.roomId()).isEqualTo("room-0-0");
        assertThat(updated.remainingMonsters()).containsExactly("spider");
        assertThat(original.discovered()).isFalse();
    }

    @Test
    @DisplayName("withCleared 호출 시 클리어 상태만 변경된 새 객체가 반환된다")
    void should_returnNewInstanceWithUpdatedCleared_when_withClearedCalled() {
        // given
        final DungeonRoomState original =
                new DungeonRoomState("room-0-0", false, true, List.of("spider"));

        // when
        final DungeonRoomState updated = original.withCleared(true);

        // then
        assertThat(updated.cleared()).isTrue();
        assertThat(updated.discovered()).isTrue();
        assertThat(updated.roomId()).isEqualTo("room-0-0");
        assertThat(updated.remainingMonsters()).containsExactly("spider");
        assertThat(original.cleared()).isFalse();
    }

    @Test
    @DisplayName("withRemainingMonsters 호출 시 몬스터 목록만 변경된 새 객체가 반환된다")
    void should_returnNewInstanceWithUpdatedMonsters_when_withRemainingMonstersCalled() {
        // given
        final DungeonRoomState original =
                new DungeonRoomState("room-0-0", false, true, List.of("spider", "goblin"));

        // when
        final DungeonRoomState updated = original.withRemainingMonsters(List.of("goblin"));

        // then
        assertThat(updated.remainingMonsters()).containsExactly("goblin");
        assertThat(updated.cleared()).isFalse();
        assertThat(updated.discovered()).isTrue();
        assertThat(original.remainingMonsters()).containsExactly("spider", "goblin");
    }

    @Test
    @DisplayName("동일한 필드를 가진 DungeonRoomState는 동등하다")
    void should_beEqual_when_allFieldsMatch() {
        // given
        final DungeonRoomState state1 =
                new DungeonRoomState("room-1-0", true, true, List.of("spider"));
        final DungeonRoomState state2 =
                new DungeonRoomState("room-1-0", true, true, List.of("spider"));

        // when & then
        assertThat(state1).isEqualTo(state2);
        assertThat(state1.hashCode()).isEqualTo(state2.hashCode());
        assertThat(state1.toString()).contains("room-1-0", "cleared=true", "discovered=true");
    }
}
