package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.application.exception.CharacterCreationException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.Vital;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 기본 캐릭터 초기값 및 생성 실패 롤백 동작을 검증하는 단위 테스트.
 *
 * <p>Base_Stats, Lv1/누적1/EXP0, 시작 노드, HP/MP/Stamina 초기값을 확인하고,
 * 저장 실패 시 {@link CharacterCreationException} 전파 및 롤백 동작을 검증한다.
 *
 * <p><b>Validates: Requirements 2.2, 2.3, 2.4, 2.7, 2.8, 2.9</b>
 */
class CharacterServiceDefaultValuesTest {

    private static final int EXPECTED_STR = 10;
    private static final int EXPECTED_DEX = 10;
    private static final int EXPECTED_INT = 10;
    private static final int EXPECTED_CRITICAL = 5;
    private static final int EXPECTED_DEFENSE = 5;
    private static final int EXPECTED_LEVEL = 1;
    private static final int EXPECTED_ACCUMULATED_LEVEL = 1;
    private static final long EXPECTED_EXPERIENCE = 0L;
    private static final String EXPECTED_START_NODE = "tir-chonaill";
    private static final int EXPECTED_HP_CURRENT = 100;
    private static final int EXPECTED_HP_MAX = 100;
    private static final int EXPECTED_MP_CURRENT = 100;
    private static final int EXPECTED_MP_MAX = 100;
    private static final int EXPECTED_STAMINA_CURRENT = 100;
    private static final int EXPECTED_STAMINA_MAX = 100;

    private final CharacterProgressRepository mockRepository = mock(CharacterProgressRepository.class);
    private final CharacterService characterService = new CharacterService(mockRepository);

    /**
     * 빈 저장소에서 생성된 캐릭터의 Base_Stats가 STR=10, DEX=10, INT=10, Critical=5, DEF=5인지 검증한다.
     */
    @Test
    @DisplayName("Req 2.2: Base_Stats 초기값 — STR=10, DEX=10, INT=10, Critical=5, DEF=5")
    void should_haveDefaultBaseStats_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        final Stats stats = result.getStats();
        assertThat(stats.str()).isEqualTo(EXPECTED_STR);
        assertThat(stats.dex()).isEqualTo(EXPECTED_DEX);
        assertThat(stats.intelligence()).isEqualTo(EXPECTED_INT);
        assertThat(stats.critical()).isEqualTo(EXPECTED_CRITICAL);
        assertThat(stats.defense()).isEqualTo(EXPECTED_DEFENSE);
    }

    /**
     * 빈 저장소에서 생성된 캐릭터의 레벨/누적레벨/경험치가 Lv1/누적1/EXP0인지 검증한다.
     */
    @Test
    @DisplayName("Req 2.3: Lv1/누적1/EXP0 초기값")
    void should_haveDefaultLevelAndExperience_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        assertThat(result.getCurrentLevel()).isEqualTo(EXPECTED_LEVEL);
        assertThat(result.getAccumulatedLevel()).isEqualTo(EXPECTED_ACCUMULATED_LEVEL);
        assertThat(result.getExperience()).isEqualTo(EXPECTED_EXPERIENCE);
    }

    /**
     * 빈 저장소에서 생성된 캐릭터의 시작 노드가 "tir-chonaill"인지 검증한다.
     */
    @Test
    @DisplayName("Req 2.4: 시작 노드 — tir-chonaill")
    void should_haveDefaultStartNode_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        assertThat(result.getCurrentNodeId()).isEqualTo(EXPECTED_START_NODE);
    }

    /**
     * 빈 저장소에서 생성된 캐릭터의 HP/MP/Stamina가 각각 100/100인지 검증한다.
     */
    @Test
    @DisplayName("Req 2.2: HP=100/100, MP=100/100, Stamina=100/100 초기값")
    void should_haveDefaultVitals_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        final Vital hp = result.getHp();
        assertThat(hp.current()).isEqualTo(EXPECTED_HP_CURRENT);
        assertThat(hp.max()).isEqualTo(EXPECTED_HP_MAX);

        final Vital mp = result.getMp();
        assertThat(mp.current()).isEqualTo(EXPECTED_MP_CURRENT);
        assertThat(mp.max()).isEqualTo(EXPECTED_MP_MAX);

        final Vital stamina = result.getStamina();
        assertThat(stamina.current()).isEqualTo(EXPECTED_STAMINA_CURRENT);
        assertThat(stamina.max()).isEqualTo(EXPECTED_STAMINA_MAX);
    }

    /**
     * save() 실패 시 CharacterCreationException이 발생하는지 검증한다.
     */
    @Test
    @DisplayName("Req 2.7: save 실패 시 CharacterCreationException 발생")
    void should_throwCharacterCreationException_when_saveFails() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenThrow(new RuntimeException("DB 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> characterService.loadOrCreateDefault())
                .isInstanceOf(CharacterCreationException.class);
    }

    /**
     * 저장 성공 시에는 CharacterCreationException이 발생하지 않는지 검증한다.
     * 저장 실패에 한해서만 오류를 반환해야 한다.
     */
    @Test
    @DisplayName("Req 2.8: 저장 성공 시 CharacterCreationException 미발생")
    void should_notThrowException_when_saveSucceeds() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertThatCode(() -> characterService.loadOrCreateDefault())
                .doesNotThrowAnyException();
    }

    /**
     * save 실패 시 CharacterCreationException에 원인 예외(cause)가 포함되어 전파되는지 검증한다.
     * 추가 복구를 시도하지 않고 예외를 그대로 전파한다.
     */
    @Test
    @DisplayName("Req 2.9: 롤백 실패 시 cause 포함 예외 전파, 추가 복구 미시도")
    void should_propagateExceptionWithCause_when_saveFailsWithCause() {
        // Given
        final RuntimeException rootCause = new RuntimeException("디스크 I/O 오류");
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenThrow(rootCause);

        // When & Then
        assertThatThrownBy(() -> characterService.loadOrCreateDefault())
                .isInstanceOf(CharacterCreationException.class)
                .hasCause(rootCause);
    }
}
