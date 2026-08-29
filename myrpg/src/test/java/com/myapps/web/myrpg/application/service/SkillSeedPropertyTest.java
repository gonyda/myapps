package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.ArgumentCaptor;

/**
 * 신규 캐릭터 시드 프로퍼티 테스트.
 *
 * <p>임의의 캐릭터 ID에 대해 {@code seedDefault(characterId)} 호출 시, 기본 스킬 4종(베기·조준 사격·마나 볼트·디펜스)이 각각 F
 * 랭크·usageCount 0으로 정확히 한 번씩 저장되는지 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 14: 신규 캐릭터 시드
 *
 * <p><b>Validates: Requirements 10.3, 15.4</b>
 */
class SkillSeedPropertyTest {

    private static final List<String> DEFAULT_SEED_SKILL_IDS =
            List.of("slash", "aimed_shot", "mana_bolt", "defense");

    /**
     * 임의의 캐릭터 ID에 대해 seedDefault 호출 시, 기본 스킬 4종이 F 랭크·카운트 0으로 정확히 한 번씩 저장된다.
     *
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 100)
    void should_seedFourDefaultSkills_when_seedDefaultCalled(
            @ForAll("characterIds") final Long characterId) {

        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);

        stubCatalogAndRepository(mockCatalog, mockRepository, characterId);

        final SkillService skillService =
                new SkillService(
                        mockRepository, mock(CharacterProgressRepository.class), mockCatalog);

        // When: 시드 실행
        skillService.seedDefault(characterId);

        // Then: 기본 스킬 4종이 정확히 한 번씩 저장된다.
        final ArgumentCaptor<CharacterSkill> captor = ArgumentCaptor.forClass(CharacterSkill.class);
        verify(mockRepository, times(DEFAULT_SEED_SKILL_IDS.size())).save(captor.capture());

        final List<CharacterSkill> saved = captor.getAllValues();
        assertThat(saved)
                .as("시드된 스킬 id는 기본 4종과 정확히 일치해야 한다")
                .extracting(CharacterSkill::getSkillId)
                .containsExactlyInAnyOrderElementsOf(DEFAULT_SEED_SKILL_IDS);

        assertThat(saved)
                .allSatisfy(
                        skill -> {
                            assertThat(skill.getCharacterId())
                                    .as("시드 스킬의 캐릭터 ID가 일치해야 한다")
                                    .isEqualTo(characterId);
                            assertThat(skill.getRank())
                                    .as("시드 스킬의 랭크는 F이어야 한다")
                                    .isEqualTo(SkillRank.F);
                            assertThat(skill.getUsageCount()).as("시드 스킬의 사용 횟수는 0이어야 한다").isZero();
                        });
    }

    private void stubCatalogAndRepository(
            final SkillCatalogService mockCatalog,
            final CharacterSkillRepository mockRepository,
            final Long characterId) {
        for (final String skillId : DEFAULT_SEED_SKILL_IDS) {
            final Skill catalogSkill =
                    "defense".equals(skillId) ? mock(DefenseSkill.class) : mock(DamageSkill.class);
            when(mockCatalog.byId(skillId)).thenReturn(Optional.of(catalogSkill));
            when(mockRepository.findByCharacterIdAndSkillId(characterId, skillId))
                    .thenReturn(Optional.empty());
        }
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
