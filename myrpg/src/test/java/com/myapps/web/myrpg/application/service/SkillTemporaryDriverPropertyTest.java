package com.myapps.web.myrpg.application.service;

import java.util.Arrays;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 임시 드라이버(fillUsageToRequirement/fillKillToRequirement)의 100% 충전을 검증하는 프로퍼티 테스트.
 *
 * <p>MASTER가 아닌 임의 랭크의 스킬에 대해:
 * <ul>
 *   <li>{@code fillUsageToRequirement} 호출 후 usageCount == 현재 랭크 requiredUsage</li>
 *   <li>{@code fillKillToRequirement} 호출 후 killCount == 현재 랭크 requiredKills</li>
 * </ul>
 *
 * <p>MASTER 랭크에서는 메서드가 조기 반환하여 상태가 변하지 않음을 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 18: 임시 드라이버 100% 충전
 *
 * <p><b>Validates: Requirements 14.2, 14.3</b>
 */
class SkillTemporaryDriverPropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String SKILL_ID = "windmill";

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * MASTER가 아닌 랭크에서 fillUsageToRequirement 호출 후 usageCount가 현재 랭크의 requiredUsage와 같다.
     *
     * @param rank MASTER가 아닌 임의 스킬 랭크
     */
    @Property(tries = 100)
    void should_setUsageToRequirement_when_fillUsageCalled(
            @ForAll("nonMasterRanks") final SkillRank rank) {

        // Given
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalogService = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalogService);

        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, SKILL_ID, rank, 0, 0);

        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();

        // When
        skillService.fillUsageToRequirement(CHARACTER_ID, SKILL_ID);

        // Then
        assertThat(skill.getUsageCount())
                .as("fillUsageToRequirement 후 usageCount는 현재 랭크 요구 사용 횟수와 같아야 한다")
                .isEqualTo(requirement.requiredUsage());
    }

    /**
     * MASTER가 아닌 랭크에서 fillKillToRequirement 호출 후 killCount가 현재 랭크의 requiredKills와 같다.
     *
     * @param rank MASTER가 아닌 임의 스킬 랭크
     */
    @Property(tries = 100)
    void should_setKillToRequirement_when_fillKillCalled(
            @ForAll("nonMasterRanks") final SkillRank rank) {

        // Given
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalogService = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalogService);

        final CharacterSkill skill = new CharacterSkill(CHARACTER_ID, SKILL_ID, rank, 0, 0);

        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();

        // When
        skillService.fillKillToRequirement(CHARACTER_ID, SKILL_ID);

        // Then
        assertThat(skill.getKillCount())
                .as("fillKillToRequirement 후 killCount는 현재 랭크 요구 막타 처치 수와 같아야 한다")
                .isEqualTo(requirement.requiredKills());
    }

    /**
     * MASTER 랭크에서 fillUsageToRequirement를 호출하면 상태가 변하지 않는다.
     */
    @Property(tries = 100)
    void should_notChangeUsage_when_rankIsMaster(
            @ForAll("masterSkillUsageCounts") final int initialUsage) {

        // Given
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalogService = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalogService);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, SkillRank.MASTER, initialUsage, 0);

        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        // When
        skillService.fillUsageToRequirement(CHARACTER_ID, SKILL_ID);

        // Then
        assertThat(skill.getUsageCount())
                .as("MASTER 랭크에서는 usageCount가 변하지 않아야 한다")
                .isEqualTo(initialUsage);
    }

    /**
     * MASTER 랭크에서 fillKillToRequirement를 호출하면 상태가 변하지 않는다.
     */
    @Property(tries = 100)
    void should_notChangeKill_when_rankIsMaster(
            @ForAll("masterSkillKillCounts") final int initialKill) {

        // Given
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalogService = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalogService);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, SkillRank.MASTER, 0, initialKill);

        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        // When
        skillService.fillKillToRequirement(CHARACTER_ID, SKILL_ID);

        // Then
        assertThat(skill.getKillCount())
                .as("MASTER 랭크에서는 killCount가 변하지 않아야 한다")
                .isEqualTo(initialKill);
    }

    /**
     * MASTER가 아닌 모든 SkillRank를 생성하는 Arbitrary.
     *
     * @return MASTER 제외 SkillRank Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> nonMasterRanks() {
        final SkillRank[] nonMasterRanks = Arrays.stream(SkillRank.values())
                .filter(rank -> !rank.isMax())
                .toArray(SkillRank[]::new);
        return Arbitraries.of(nonMasterRanks);
    }

    /**
     * MASTER 랭크 스킬의 임의 사용 횟수를 생성하는 Arbitrary.
     *
     * @return 0~10000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> masterSkillUsageCounts() {
        return Arbitraries.integers().between(0, 10000);
    }

    /**
     * MASTER 랭크 스킬의 임의 막타 처치 수를 생성하는 Arbitrary.
     *
     * @return 0~10000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> masterSkillKillCounts() {
        return Arbitraries.integers().between(0, 10000);
    }
}
