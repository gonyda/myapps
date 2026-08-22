package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link DungeonProgressEntity} JPA 엔티티 단위 테스트. */
class DungeonProgressEntityTest {

    @Test
    @DisplayName("생성자로 초기화된 필드들이 올바르게 조회된다")
    void should_initializeAllFields_when_constructorCalled() {
        // given
        final Long characterId = 100L;
        final String dungeonId = "alby";
        final String entranceNodeId = "alby-entrance";
        final String startRoomId = "room-0-0";
        final String bossRoomId = "room-10-0";
        final String currentRoomId = "room-0-0";
        final String graphJson = "{\"nodes\":[]}";
        final String roomStatesJson = "{\"room-0-0\":{}}";
        final LocalDateTime createdAt = LocalDateTime.of(2026, 8, 22, 12, 0);
        final LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 22, 12, 30);

        // when
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        characterId,
                        dungeonId,
                        entranceNodeId,
                        startRoomId,
                        bossRoomId,
                        currentRoomId,
                        graphJson,
                        roomStatesJson,
                        createdAt,
                        updatedAt);

        // then
        assertThat(entity.getCharacterId()).isEqualTo(characterId);
        assertThat(entity.getDungeonId()).isEqualTo(dungeonId);
        assertThat(entity.getEntranceNodeId()).isEqualTo(entranceNodeId);
        assertThat(entity.getStartRoomId()).isEqualTo(startRoomId);
        assertThat(entity.getBossRoomId()).isEqualTo(bossRoomId);
        assertThat(entity.getCurrentRoomId()).isEqualTo(currentRoomId);
        assertThat(entity.getDungeonGraphJson()).isEqualTo(graphJson);
        assertThat(entity.getRoomStatesJson()).isEqualTo(roomStatesJson);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("보조 생성자 사용 시 createdAt과 updatedAt이 현재 시각으로 자동 설정된다")
    void should_setDefaultTimestamps_when_compactConstructorCalled() {
        // given
        final Long characterId = 200L;

        // when
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        characterId,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-10-0",
                        "room-0-0",
                        "{}",
                        "{}");

        // then
        assertThat(entity.getCharacterId()).isEqualTo(200L);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("세터를 통해 방 위치, 그래프 JSON, 방 상태 JSON 및 수정 일시를 갱신할 수 있다")
    void should_updateFields_when_setterCalled() {
        // given
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-10-0",
                        "room-0-0",
                        "{}",
                        "{}");

        final String newRoomId = "room-1-0";
        final String newGraphJson = "{\"nodes\":[{\"id\":\"room-1-0\"}]}";
        final String newStatesJson = "{\"room-1-0\":{\"cleared\":true}}";
        final LocalDateTime newUpdatedAt = LocalDateTime.of(2026, 8, 22, 15, 0);

        // when
        entity.setCurrentRoomId(newRoomId);
        entity.setDungeonGraphJson(newGraphJson);
        entity.setRoomStatesJson(newStatesJson);
        entity.setUpdatedAt(newUpdatedAt);

        // then
        assertThat(entity.getCurrentRoomId()).isEqualTo(newRoomId);
        assertThat(entity.getDungeonGraphJson()).isEqualTo(newGraphJson);
        assertThat(entity.getRoomStatesJson()).isEqualTo(newStatesJson);
        assertThat(entity.getUpdatedAt()).isEqualTo(newUpdatedAt);
    }

    @Test
    @DisplayName("onCreate 및 onUpdate 라이프사이클 메서드가 호출되면 타임스탬프가 갱신된다")
    void should_updateTimestamps_when_lifecycleCallbacksInvoked() {
        // given
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-10-0",
                        "room-0-0",
                        "{}",
                        "{}");

        // when
        entity.onCreate();
        final LocalDateTime initialUpdated = entity.getUpdatedAt();

        entity.onUpdate();

        // then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull().isAfterOrEqualTo(initialUpdated);
    }
}
