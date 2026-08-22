package com.myapps.web.myrpg.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/** {@link DungeonProgressRepository} Spring Data JPA 레포지토리 슬라이스 테스트. */
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DungeonProgressRepositoryTest {

    private final DungeonProgressRepository dungeonProgressRepository;
    private final TestEntityManager entityManager;

    DungeonProgressRepositoryTest(
            final DungeonProgressRepository dungeonProgressRepository,
            final TestEntityManager entityManager) {
        this.dungeonProgressRepository = dungeonProgressRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("DungeonProgressEntity를 저장하고 findByCharacterId로 정상 조회할 수 있다")
    void should_saveAndFindByCharacterId_when_entityPersisted() {
        // given
        final Long characterId = 42L;
        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        characterId,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-10-0",
                        "room-0-0",
                        "{\"nodes\":[{\"id\":\"room-0-0\"}]}",
                        "{\"room-0-0\":{\"cleared\":true}}");

        entityManager.persistAndFlush(entity);
        entityManager.clear();

        // when
        final Optional<DungeonProgressEntity> found =
                dungeonProgressRepository.findByCharacterId(characterId);

        // then
        assertThat(found).isPresent();
        final DungeonProgressEntity loaded = found.get();
        assertThat(loaded.getCharacterId()).isEqualTo(characterId);
        assertThat(loaded.getDungeonId()).isEqualTo("alby");
        assertThat(loaded.getEntranceNodeId()).isEqualTo("alby-entrance");
        assertThat(loaded.getStartRoomId()).isEqualTo("room-0-0");
        assertThat(loaded.getBossRoomId()).isEqualTo("room-10-0");
        assertThat(loaded.getCurrentRoomId()).isEqualTo("room-0-0");
        assertThat(loaded.getDungeonGraphJson()).contains("room-0-0");
        assertThat(loaded.getRoomStatesJson()).contains("cleared");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 characterId로 조회 시 빈 Optional이 반환된다")
    void should_returnEmpty_when_characterIdNotFound() {
        // given
        final Long nonExistentCharacterId = 99999L;

        // when
        final Optional<DungeonProgressEntity> found =
                dungeonProgressRepository.findByCharacterId(nonExistentCharacterId);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("deleteByCharacterId 호출 시 해당 캐릭터의 던전 진행 데이터가 삭제된다")
    void should_deleteEntity_when_deleteByCharacterIdCalled() {
        // given
        final Long characterId = 77L;
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

        entityManager.persistAndFlush(entity);
        entityManager.clear();

        // when
        dungeonProgressRepository.deleteByCharacterId(characterId);
        entityManager.flush();
        entityManager.clear();

        // then
        final Optional<DungeonProgressEntity> found =
                dungeonProgressRepository.findByCharacterId(characterId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 던전의 현재 방 및 상태 JSON을 갱신하고 재조회하면 변경사항이 영속화된다")
    void should_persistUpdates_when_modifiedAndFlushed() {
        // given
        final Long characterId = 88L;
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

        entityManager.persistAndFlush(entity);
        final Long entityId = entity.getId();
        entityManager.clear();

        // when
        final DungeonProgressEntity loaded =
                entityManager.find(DungeonProgressEntity.class, entityId);
        loaded.setCurrentRoomId("room-1-0");
        loaded.setRoomStatesJson(
                "{\"room-0-0\":{\"cleared\":true},\"room-1-0\":{\"discovered\":true}}");
        entityManager.persistAndFlush(loaded);
        entityManager.clear();

        // then
        final Optional<DungeonProgressEntity> reloaded =
                dungeonProgressRepository.findByCharacterId(characterId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getCurrentRoomId()).isEqualTo("room-1-0");
        assertThat(reloaded.get().getRoomStatesJson()).contains("room-1-0");
    }
}
