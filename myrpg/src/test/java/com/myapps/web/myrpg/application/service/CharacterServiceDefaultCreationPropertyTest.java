package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import org.mockito.ArgumentCaptor;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 빈 저장소에서 기본 캐릭터가 정확히 한 번 생성되는지 검증하는 프로퍼티 테스트.
 *
 * <p>빈 {@code Character_Store}에서 {@code loadOrCreateDefault()} 호출 시
 * 닉네임 "고니" 캐릭터가 정확히 한 번 저장되는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 13: 빈 저장소 시 기본 캐릭터 단일 생성
 *
 * <p><b>Validates: Requirements 2.1, 2.5</b>
 */
class CharacterServiceDefaultCreationPropertyTest {

    private static final String EXPECTED_NICKNAME = "고니";

    /**
     * 빈 저장소에서 loadOrCreateDefault() 호출 시 닉네임 "고니" 캐릭터가
     * 정확히 한 번 save 되는지 검증한다.
     *
     * @param tries jqwik이 생성하는 임의 정수 (테스트 반복 다양성 부여용)
     */
    @Property(tries = 100)
    void should_saveDefaultCharacterExactlyOnce_when_storeIsEmpty(
            @ForAll @IntRange(min = 1, max = 1000) final int tries) {
        // Given: 빈 저장소 (findFirstByOrderByIdAsc가 Optional.empty() 반환)
        final CharacterProgressRepository mockRepository = mock(CharacterProgressRepository.class);
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(mockRepository.save(org.mockito.ArgumentMatchers.any(CharacterProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final CharacterService characterService = new CharacterService(mockRepository);

        // When: loadOrCreateDefault 호출
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then: save가 정확히 한 번 호출됨
        final ArgumentCaptor<CharacterProgress> captor = ArgumentCaptor.forClass(CharacterProgress.class);
        verify(mockRepository, times(1)).save(captor.capture());

        // Then: 저장된 캐릭터의 닉네임이 "고니"
        final CharacterProgress savedCharacter = captor.getValue();
        assertThat(savedCharacter.getNickname()).isEqualTo(EXPECTED_NICKNAME);

        // Then: 반환된 캐릭터도 닉네임이 "고니"
        assertThat(result.getNickname()).isEqualTo(EXPECTED_NICKNAME);
    }
}
