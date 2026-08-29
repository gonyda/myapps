package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.exception.CharacterCreationException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기본 캐릭터 초기값 및 생성 실패 롤백 동작을 검증하는 단위 테스트.
 *
 * <p>Lv1/누적1/EXP0, 시작 노드, HP/MP/Stamina 현재값, 재능/환생 초기값을 확인하고, 저장 실패 시 {@link
 * CharacterCreationException} 전파 및 롤백 동작을 검증한다. 신규 캐릭터 생성 시 windmill F 스킬 시드 호출도 검증한다.
 *
 * <p><b>Validates: Requirements 2.2, 2.3, 2.4, 2.7, 2.8, 2.9, 10.3, 15.4</b>
 */
class CharacterServiceDefaultValuesTest {

    private static final int EXPECTED_LEVEL = 1;
    private static final int EXPECTED_ACCUMULATED_LEVEL = 1;
    private static final long EXPECTED_EXPERIENCE = 0L;
    private static final String EXPECTED_START_NODE = "tir-chonaill";
    private static final int EXPECTED_HP_CURRENT = 100;
    private static final int EXPECTED_MP_CURRENT = 100;
    private static final int EXPECTED_STAMINA_CURRENT = 100;
    private static final Long SAVED_CHARACTER_ID = 1L;

    private final CharacterProgressRepository mockRepository =
            mock(CharacterProgressRepository.class);
    private final SkillService mockSkillService = mock(SkillService.class);
    private final InventoryService mockInventoryService = mock(InventoryService.class);
    private final CharacterService characterService =
            new CharacterService(mockRepository, mockSkillService, mockInventoryService);

    /** 빈 저장소에서 생성된 캐릭터의 레벨/누적레벨/경험치가 Lv1/누적1/EXP0인지 검증한다. */
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

    /** 빈 저장소에서 생성된 캐릭터의 시작 노드가 "tir-chonaill"인지 검증한다. */
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

    /** 빈 저장소에서 생성된 캐릭터의 HP/MP/Stamina 현재값이 각각 100인지 검증한다. */
    @Test
    @DisplayName("Req 2.2: HP=100, MP=100, Stamina=100 현재값 초기값")
    void should_haveDefaultVitalCurrentValues_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        assertThat(result.getHpCurrent()).isEqualTo(EXPECTED_HP_CURRENT);
        assertThat(result.getMpCurrent()).isEqualTo(EXPECTED_MP_CURRENT);
        assertThat(result.getStaminaCurrent()).isEqualTo(EXPECTED_STAMINA_CURRENT);
    }

    /** 빈 저장소에서 생성된 캐릭터의 재능이 MELEE이고 환생 기록이 없는지 검증한다. */
    @Test
    @DisplayName("Req 2.2: 재능 MELEE, lastRebirthAt null 초기값")
    void should_haveDefaultTalent_when_storeIsEmpty() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then
        assertThat(result.getTalent()).isEqualTo(TalentType.MELEE);
        assertThat(result.getLastRebirthAt()).isNull();
    }

    /** save() 실패 시 CharacterCreationException이 발생하는지 검증한다. */
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

    /** 저장 성공 시에는 CharacterCreationException이 발생하지 않는지 검증한다. 저장 실패에 한해서만 오류를 반환해야 한다. */
    @Test
    @DisplayName("Req 2.8: 저장 성공 시 CharacterCreationException 미발생")
    void should_notThrowException_when_saveSucceeds() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertThatCode(() -> characterService.loadOrCreateDefault()).doesNotThrowAnyException();
    }

    /**
     * save 실패 시 CharacterCreationException에 원인 예외(cause)가 포함되어 전파되는지 검증한다. 추가 복구를 시도하지 않고 예외를 그대로
     * 전파한다.
     */
    @Test
    @DisplayName("Req 2.9: 롤백 실패 시 cause 포함 예외 전파, 추가 복구 미시도")
    void should_propagateExceptionWithCause_when_saveFailsWithCause() {
        // Given
        final RuntimeException rootCause = new RuntimeException("디스크 I/O 오류");
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class))).thenThrow(rootCause);

        // When & Then
        assertThatThrownBy(() -> characterService.loadOrCreateDefault())
                .isInstanceOf(CharacterCreationException.class)
                .hasCause(rootCause);
    }

    /** 신규 캐릭터 생성 시 windmill 스킬을 시드하기 위해 seedDefault가 호출되는지 검증한다. */
    @Test
    @DisplayName("Req 10.3, 15.4: 신규 캐릭터 생성 시 skillService.seedDefault 호출")
    void should_callSeedDefault_when_newCharacterCreated() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(
                        invocation -> {
                            final CharacterProgress saved = invocation.getArgument(0);
                            setId(saved, SAVED_CHARACTER_ID);
                            return saved;
                        });

        // When
        characterService.loadOrCreateDefault();

        // Then
        verify(mockSkillService).seedDefault(SAVED_CHARACTER_ID);
    }

    /** 신규 캐릭터 생성 시 기본 아이템 시드를 위해 inventoryService.seedDefault가 호출되는지 검증한다. */
    @Test
    @DisplayName("Req 18.1: 신규 캐릭터 생성 시 inventoryService.seedDefault 호출")
    void should_callInventorySeedDefault_when_newCharacterCreated() {
        // Given
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(any(CharacterProgress.class)))
                .thenAnswer(
                        invocation -> {
                            final CharacterProgress saved = invocation.getArgument(0);
                            setId(saved, SAVED_CHARACTER_ID);
                            return saved;
                        });

        // When
        characterService.loadOrCreateDefault();

        // Then
        verify(mockInventoryService).seedDefault(SAVED_CHARACTER_ID);
    }

    /** 기존 캐릭터가 이미 존재할 때에는 seedDefault가 호출되지 않는지 검증한다. */
    @Test
    @DisplayName("Req 10.3: 기존 캐릭터 로드 시 seedDefault 미호출")
    void should_notCallSeedDefault_when_existingCharacterLoaded() {
        // Given
        final CharacterProgress existing = CharacterProgress.createDefault();
        setId(existing, SAVED_CHARACTER_ID);
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));

        // When
        characterService.loadOrCreateDefault();

        // Then
        verify(mockSkillService, never()).seedDefault(any());
    }

    /** 기존 캐릭터가 이미 존재할 때에는 inventoryService.seedDefault가 호출되지 않는지 검증한다. */
    @Test
    @DisplayName("Req 18.1: 기존 캐릭터 로드 시 inventoryService.seedDefault 미호출")
    void should_notCallInventorySeedDefault_when_existingCharacterLoaded() {
        // Given
        final CharacterProgress existing = CharacterProgress.createDefault();
        setId(existing, SAVED_CHARACTER_ID);
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));

        // When
        characterService.loadOrCreateDefault();

        // Then
        verify(mockInventoryService, never()).seedDefault(any());
    }

    /**
     * 리플렉션으로 CharacterProgress의 id 필드를 설정한다 (테스트 전용).
     *
     * @param progress 대상 엔티티
     * @param id 설정할 ID 값
     */
    private void setId(final CharacterProgress progress, final Long id) {
        try {
            final java.lang.reflect.Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("테스트 ID 설정 실패", exception);
        }
    }
}
