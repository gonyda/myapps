package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 랭크업 트랜잭션 효과를 검증하는 프로퍼티 테스트.
 *
 * <p>Rankable 상태(조건 충족 + AP 충족 + 랭크 ≠ MASTER)에서 랭크업 수행 후:
 * (a) rank == 이전.next(), (b) usageCount == 0, (c) killCount == 0,
 * (d) abilityPoints == 이전 AP - apCost(이전 랭크)임을 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 9: 랭크업 트랜잭션 효과
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 6.3</b>
 */
class SkillRankUpEffectPropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String SKILL_ID = "windmill";

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * Rankable 상태에서 랭크업을 수행하면 rank=next, usage=0, kill=0, ap=이전-cost가 된다.
     *
     * @param rankableState 랭크업 가능한 상태(랭크, 사용 횟수, 막타 처치 수, AP)
     */
    @Property(tries = 100)
    void should_applyTransactionEffects_when_rankUpSucceeds(
            @ForAll("rankableStates") final RankableState rankableState) {

        // Given: Rankable 상태의 스킬과 캐릭터
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalogService = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepository, mock(CharacterProgressRepository.class), mockCatalogService);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID,
                rankableState.rank(),
                rankableState.usageCount(),
                rankableState.killCount());

        final CharacterProgress progress = createProgressWithAp(rankableState.abilityPoints());

        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockRepository.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final SkillRank previousRank = rankableState.rank();
        final int previousAp = rankableState.abilityPoints();
        final int apCost = skillRankPolicy.apCost(previousRank).orElseThrow();
        final SkillRank expectedNextRank = previousRank.next().orElseThrow();

        // When: 랭크업 수행
        final boolean result = skillService.rankUp(progress, SKILL_ID);

        // Then: 트랜잭션 효과 검증
        assertThat(result)
                .as("Rankable 상태에서 랭크업은 성공해야 한다")
                .isTrue();

        assertThat(skill.getRank())
                .as("랭크는 이전 랭크의 next()여야 한다")
                .isEqualTo(expectedNextRank);

        assertThat(skill.getUsageCount())
                .as("사용 횟수는 0으로 리셋되어야 한다")
                .isZero();

        assertThat(skill.getKillCount())
                .as("막타 처치 수는 0으로 리셋되어야 한다")
                .isZero();

        assertThat(progress.getAbilityPoints())
                .as("AP는 이전 AP - apCost여야 한다")
                .isEqualTo(previousAp - apCost);
    }

    /**
     * Rankable 상태 생성기.
     *
     * <p>MASTER가 아닌 모든 랭크에 대해, 해당 랭크의 요구치 이상의 사용/막타와
     * apCost 이상의 AP를 가진 상태를 생성한다.
     *
     * @return RankableState Arbitrary
     */
    @Provide
    Arbitrary<RankableState> rankableStates() {
        final SkillRank[] nonMasterRanks = Arrays.stream(SkillRank.values())
                .filter(rank -> !rank.isMax())
                .toArray(SkillRank[]::new);

        return Arbitraries.of(nonMasterRanks).flatMap(rank -> {
            final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

            final Arbitrary<Integer> usageArbitrary = Arbitraries.integers()
                    .between(requirement.requiredUsage(), requirement.requiredUsage() + 100);
            final Arbitrary<Integer> killArbitrary = Arbitraries.integers()
                    .between(requirement.requiredKills(), requirement.requiredKills() + 100);
            final Arbitrary<Integer> apArbitrary = Arbitraries.integers()
                    .between(apCost, apCost + 200);

            return Combinators.combine(usageArbitrary, killArbitrary, apArbitrary)
                    .as((usage, kill, ap) -> new RankableState(rank, usage, kill, ap));
        });
    }

    private CharacterProgress createProgressWithAp(final int abilityPoints) {
        final CharacterProgress progress = new CharacterProgress(
                "테스트", 1, 50, 0L,
                TalentType.MELEE,
                null, 100, 100, 100, "tir-chonaill", abilityPoints, 0L);
        setId(progress, CHARACTER_ID);
        return progress;
    }

    private void setId(final CharacterProgress progress, final Long id) {
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("id 설정 실패", exception);
        }
    }

    /**
     * 랭크업 가능 상태를 나타내는 테스트용 레코드.
     *
     * @param rank         현재 랭크 (MASTER 아님)
     * @param usageCount   사용 횟수 (요구치 이상)
     * @param killCount    막타 처치 수 (요구치 이상)
     * @param abilityPoints 보유 AP (apCost 이상)
     */
    record RankableState(SkillRank rank, int usageCount, int killCount, int abilityPoints) {
    }
}
