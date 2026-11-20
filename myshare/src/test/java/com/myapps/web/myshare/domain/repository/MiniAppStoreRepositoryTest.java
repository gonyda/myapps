package com.myapps.web.myshare.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myshare.domain.model.MiniAppStore;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/** {@link MiniAppStoreRepository} 및 {@link MiniAppStore} 엔티티 매핑 검증 슬라이스 테스트. */
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MiniAppStoreRepositoryTest {

    private final TestEntityManager entityManager;
    private final MiniAppStoreRepository repository;

    MiniAppStoreRepositoryTest(
            final TestEntityManager entityManager, final MiniAppStoreRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    @Test
    @DisplayName("신규 미니앱 JSON을 저장하고 조회하면 app_key, CLOB data_json, updated_at이 보존된다")
    void should_saveAndFindEntity_when_validMiniAppStoreProvided() {
        // given
        final LocalDateTime savedAt = LocalDateTime.of(2026, 11, 20, 21, 30, 0);
        final String largeJson = "{\"items\":\"" + "x".repeat(5000) + "\"}";
        final MiniAppStore entity = new MiniAppStore("todo_app-1", largeJson, savedAt);

        // when
        repository.saveAndFlush(entity);
        entityManager.clear();
        final Optional<MiniAppStore> found = repository.findById("todo_app-1");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAppKey()).isEqualTo("todo_app-1");
        assertThat(found.get().getDataJson()).isEqualTo(largeJson);
        assertThat(found.get().getUpdatedAt()).isEqualTo(savedAt);
    }

    @Test
    @DisplayName("기존 미니앱 엔티티를 갱신하면 data_json과 updated_at이 업데이트된다")
    void should_updateJsonAndTimestamp_when_existingEntityUpdated() {
        // given
        final LocalDateTime initialTime = LocalDateTime.of(2026, 11, 20, 21, 0, 0);
        final LocalDateTime updatedTime = LocalDateTime.of(2026, 11, 20, 22, 0, 0);
        entityManager.persistAndFlush(new MiniAppStore("habit", "{\"v\":1}", initialTime));
        entityManager.clear();

        // when
        final MiniAppStore existing = repository.findById("habit").orElseThrow();
        existing.updateDataJson("{\"v\":2}", updatedTime);
        repository.saveAndFlush(existing);
        entityManager.clear();
        final MiniAppStore reloaded = repository.findById("habit").orElseThrow();

        // then
        assertThat(reloaded.getDataJson()).isEqualTo("{\"v\":2}");
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedTime);
    }
}
