package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.ArgumentCaptor;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 신규 캐릭터 시드 프로퍼티 테스트.
 *
 * <p>임의의 캐릭터 ID에 대해 {@code seedDefault(characterId)} 호출 시,
 * 정확히 windmill 1개가 F 랭크·usageCount 0·killCount 0으로 저장되는지 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 14: 신규 캐릭터 시드
 *
 * <p><b>Validates: Requirements 10.3, 15.4</b>
 */
class SkillSeedPropertyTest {

    private static final String WINDMILL_SKILL_ID = "windmill";

    /**
     * 임의의 캐릭터 ID에 대해 seedDefault 호출 시,
     * windmill 스킬이 F 랭크·카운트 0으로 정확히 1개 저장된다.
     *
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 100)
    void should_seedExactlyOneWindmillSkill_when_seedDefaultCalled(
            @ForAll("characterIds") final Long characterId) {

        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);

        // windmill이 카탈로그에 존재하고, 아직 보유하지 않음
        final Skill windmill = mock(DamageSkill.class);
        when(mockCatalog.byId(WINDMILL_SKILL_ID)).thenReturn(Optional.of(windmill));
        when(mockRepository.findByCharacterIdAndSkillId(characterId, WINDMILL_SKILL_ID))
                .thenReturn(Optional.empty());
        when(mockRepository.save(org.mockito.ArgumentMatchers.any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalog);

        // When: 시드 실행
        skillService.seedDefault(characterId);

        // Then: 정확히 1번 save 호출
        final ArgumentCaptor<CharacterSkill> captor = ArgumentCaptor.forClass(CharacterSkill.class);
        verify(mockRepository).save(captor.capture());

        final CharacterSkill saved = captor.getValue();
        assertThat(saved.getSkillId())
                .as("시드 스킬은 windmill이어야 한다")
                .isEqualTo(WINDMILL_SKILL_ID);
        assertThat(saved.getCharacterId())
                .as("시드 스킬의 캐릭터 ID가 일치해야 한다")
                .isEqualTo(characterId);
        assertThat(saved.getRank())
                .as("시드 스킬의 랭크는 F이어야 한다")
                .isEqualTo(SkillRank.F);
        assertThat(saved.getUsageCount())
                .as("시드 스킬의 사용 횟수는 0이어야 한다")
                .isZero();
        assertThat(saved.getKillCount())
                .as("시드 스킬의 막타 처치 수는 0이어야 한다")
                .isZero();
    }

    /**
     * 임의의 캐릭터 ID 생성기 (1 ~ 10000).
     *
     * @return 캐릭터 ID Arbitrary
     */
    @Provide
    Arbitrary<Long> characterIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }
}
