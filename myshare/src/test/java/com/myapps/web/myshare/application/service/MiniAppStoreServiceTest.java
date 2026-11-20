package com.myapps.web.myshare.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import com.myapps.web.myshare.domain.exception.InvalidMiniAppPayloadException;
import com.myapps.web.myshare.domain.model.MiniAppStore;
import com.myapps.web.myshare.domain.repository.MiniAppStoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/** {@link MiniAppStoreService} 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class MiniAppStoreServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-20T12:00:00Z");

    @Mock private MiniAppStoreRepository repository;

    private MiniAppStoreService service;

    @BeforeEach
    void setUp() {
        final MiniAppJsonValidator validator =
                new MiniAppJsonValidator(1_048_576, new ObjectMapper());
        final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        service = new MiniAppStoreService(repository, validator, fixedClock);
    }

    @Test
    @DisplayName("저장된 데이터가 존재하면 GET 조회 시 원본 JSON을 그대로 반환한다")
    void should_returnStoredJson_when_appKeyExists() {
        // given
        final MiniAppStore stored =
                new MiniAppStore(
                        "habit-tracker",
                        "{\"streak\":7,\"items\":[1,2]}",
                        LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        given(repository.findById("habit-tracker")).willReturn(Optional.of(stored));

        // when
        final String result = service.loadJson("habit-tracker");

        // then
        assertThat(result).isEqualTo("{\"streak\":7,\"items\":[1,2]}");
    }

    @Test
    @DisplayName("저장된 데이터가 없으면 GET 조회 시 빈 JSON 객체 {}를 반환한다")
    void should_returnEmptyJsonObject_when_appKeyMissing() {
        // given
        given(repository.findById("new-app")).willReturn(Optional.empty());

        // when
        final String result = service.loadJson("new-app");

        // then
        assertThat(result).isEqualTo("{}");
    }

    @Test
    @DisplayName("미존재 appKey에 유효한 JSON으로 PUT 호출 시 신규 엔티티를 삽입한다")
    void should_insertNewEntity_when_upsertCalledForNewAppKey() {
        // given
        final String payload = "{\"todos\":[{\"title\":\"study\"}]}";
        given(repository.findById("todo_app")).willReturn(Optional.empty());

        // when
        final String saved = service.upsertJson("todo_app", payload);

        // then
        assertThat(saved).isEqualTo(payload);
        final ArgumentCaptor<MiniAppStore> captor = ArgumentCaptor.forClass(MiniAppStore.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().getAppKey()).isEqualTo("todo_app");
        assertThat(captor.getValue().getDataJson()).isEqualTo(payload);
        assertThat(captor.getValue().getUpdatedAt())
                .isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("기존 appKey에 유효한 JSON으로 PUT 호출 시 data_json과 updated_at을 갱신한다")
    void should_updateExistingEntity_when_upsertCalledForExistingAppKey() {
        // given
        final LocalDateTime oldTime = LocalDateTime.of(2026, 11, 19, 10, 0, 0);
        final MiniAppStore existing = new MiniAppStore("memo", "{\"v\":1}", oldTime);
        given(repository.findById("memo")).willReturn(Optional.of(existing));

        // when
        final String saved = service.upsertJson("memo", "{\"v\":2}");

        // then
        assertThat(saved).isEqualTo("{\"v\":2}");
        assertThat(existing.getDataJson()).isEqualTo("{\"v\":2}");
        assertThat(existing.getUpdatedAt())
                .isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        then(repository).should().save(existing);
    }

    @Test
    @DisplayName("잘못된 appKey나 잘못된 JSON 입력 시 리포지토리 저장을 수행하지 않고 400 예외를 던진다")
    void should_throwExceptionWithoutSaving_when_keyOrJsonInvalid() {
        // given & when & then
        assertThatThrownBy(() -> service.loadJson("InvalidKey"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> service.upsertJson("bad.key", "{\"ok\":true}"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> service.upsertJson("valid-key", "{broken"))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
        then(repository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하는 appKey 삭제 시 엔티티를 삭제하고 true를 반환한다")
    void should_deleteExistingEntityAndReturnTrue_when_deleteAppCalled() {
        // given
        final MiniAppStore stored =
                new MiniAppStore(
                        "todo",
                        "{\"v\":1}",
                        LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        given(repository.findById("todo")).willReturn(Optional.of(stored));

        // when
        final boolean deleted = service.deleteApp("todo");

        // then
        assertThat(deleted).isTrue();
        then(repository).should().delete(stored);
    }

    @Test
    @DisplayName("이미 없는 appKey 삭제 시 예외 없이 false를 반환해 멱등성을 보장한다")
    void should_returnFalseIdempotently_when_deleteAppCalledForMissingKey() {
        // given
        given(repository.findById("missing")).willReturn(Optional.empty());

        // when
        final boolean deleted = service.deleteApp("missing");

        // then
        assertThat(deleted).isFalse();
        then(repository).should(never()).delete(any());
    }

    @Test
    @DisplayName("유효하지 않은 appKey로 삭제 요청 시 리포지토리를 호출하지 않고 예외를 던진다")
    void should_throwInvalidMiniAppKeyException_when_deleteAppCalledWithInvalidKey() {
        // given & when & then
        assertThatThrownBy(() -> service.deleteApp("BadKey"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        then(repository).shouldHaveNoInteractions();
    }
}
