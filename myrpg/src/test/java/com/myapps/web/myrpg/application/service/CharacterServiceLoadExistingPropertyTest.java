package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 기존 진행상황이 존재할 때 신규 생성 없이 기존 데이터를 반환하는지 검증하는 프로퍼티 테스트.
 *
 * <p>1개 이상의 {@code CharacterProgress}가 저장소에 존재하면
 * {@code loadOrCreateDefault()} 호출 시 새 캐릭터를 생성/저장하지 않고
 * 기존 진행상황을 그대로 반환해야 한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 14: 기존 진행상황 로드(재생성 없음)
 *
 * <p><b>Validates: Requirements 2.6</b>
 */
class CharacterServiceLoadExistingPropertyTest {

    /**
     * 저장소에 1개 이상의 CharacterProgress가 존재할 때
     * loadOrCreateDefault()는 기존 진행상황을 반환하고 save()를 호출하지 않는다.
     *
     * @param nickname         임의 닉네임
     * @param currentLevel     임의 현재 레벨
     * @param accumulatedLevel 임의 누적 레벨
     * @param experience       임의 경험치
     * @param hpCurrent        임의 HP 현재값
     * @param mpCurrent        임의 MP 현재값
     * @param staminaCurrent   임의 Stamina 현재값
     */
    @Property(tries = 100)
    void should_returnExistingProgress_when_storeHasCharacter(
            @ForAll @StringLength(min = 1, max = 10) final String nickname,
            @ForAll @IntRange(min = 1, max = 200) final int currentLevel,
            @ForAll @IntRange(min = 1, max = 200) final int accumulatedLevel,
            @ForAll @LongRange(min = 0, max = 100_000) final long experience,
            @ForAll @IntRange(min = 0, max = 999) final int hpCurrent,
            @ForAll @IntRange(min = 0, max = 999) final int mpCurrent,
            @ForAll @IntRange(min = 0, max = 999) final int staminaCurrent) {

        // Given: 저장소에 이미 존재하는 CharacterProgress
        final CharacterProgress existingProgress = new CharacterProgress(
                nickname, currentLevel, accumulatedLevel, experience,
                TalentType.MELEE, null, hpCurrent, mpCurrent, staminaCurrent, "tir-chonaill", 0);

        final CharacterProgressRepository mockRepository = mock(CharacterProgressRepository.class);
        when(mockRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingProgress));

        final SkillService mockSkillService = mock(SkillService.class);
        final CharacterService characterService = new CharacterService(mockRepository, mockSkillService);

        // When: loadOrCreateDefault 호출
        final CharacterProgress result = characterService.loadOrCreateDefault();

        // Then: save()가 절대 호출되지 않음 (신규 생성 없음)
        verify(mockRepository, never()).save(org.mockito.ArgumentMatchers.any(CharacterProgress.class));

        // Then: 반환된 객체가 기존 진행상황과 동일
        assertThat(result).isSameAs(existingProgress);
    }
}
