package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** DEFENSE 타입 스킬의 승급이 사용 횟수 및 AP만으로 정상 동작함을 검증하는 프로퍼티 테스트. */
class SkillRankUpDefenseKillExemptPropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String DEFENSE_SKILL_ID = "defense";
    private static final int MAX_USAGE_SURPLUS = 10;
    private static final int MAX_AP_SURPLUS = 10;

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * DEFENSE 스킬은 usage와 AP만 충족하면 랭크업이 성공한다.
     *
     * @param state usage·AP 충족 상태
     */
    @Property(tries = 100)
    void should_succeedRankUp_when_defenseSkillAndConditionsMet(
            @ForAll("defenseRankableState") final DefenseRankUpState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService =
                new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill =
                new CharacterSkill(
                        CHARACTER_ID, DEFENSE_SKILL_ID, state.rank(), state.usageCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, DEFENSE_SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockRepo.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mockCatalog.byId(DEFENSE_SKILL_ID))
                .thenReturn(Optional.of(createDefenseSkillCatalog()));

        final boolean result = skillService.rankUp(progress, DEFENSE_SKILL_ID);

        assertThat(result).as("DEFENSE 스킬은 usage+AP 충족 시 랭크업 성공").isTrue();
    }

    /**
     * DEFENSE 스킬이라도 usage가 부족하면 랭크업이 실패한다.
     *
     * @param state usage 부족 상태
     */
    @Property(tries = 100)
    void should_failRankUp_when_defenseSkillAndUsageInsufficient(
            @ForAll("defenseUsageInsufficientState") final DefenseRankUpState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService =
                new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill =
                new CharacterSkill(
                        CHARACTER_ID, DEFENSE_SKILL_ID, state.rank(), state.usageCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, DEFENSE_SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockCatalog.byId(DEFENSE_SKILL_ID))
                .thenReturn(Optional.of(createDefenseSkillCatalog()));

        final boolean result = skillService.rankUp(progress, DEFENSE_SKILL_ID);

        assertThat(result).as("DEFENSE 스킬이라도 usage 부족 시 랭크업 실패").isFalse();
    }

    // ── Providers ──

    /**
     * DEFENSE 스킬 승급 가능 상태: ≠MASTER, usage ≥ 요구치, AP ≥ apCost.
     *
     * @return Rankable 상태 Arbitrary
     */
    @Provide
    Arbitrary<DefenseRankUpState> defenseRankableState() {
        return nonMasterRank()
                .flatMap(
                        rank -> {
                            final RankUpRequirement requirement =
                                    skillRankPolicy.requirement(rank).orElseThrow();
                            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

                            final Arbitrary<Integer> usages =
                                    Arbitraries.integers()
                                            .between(
                                                    requirement.requiredUsage(),
                                                    requirement.requiredUsage()
                                                            + MAX_USAGE_SURPLUS);
                            final Arbitrary<Integer> aps =
                                    Arbitraries.integers().between(apCost, apCost + MAX_AP_SURPLUS);

                            return Combinators.combine(usages, aps)
                                    .as((usage, ap) -> new DefenseRankUpState(rank, usage, ap));
                        });
    }

    /**
     * DEFENSE 스킬 usage 부족 상태: ≠MASTER, usage < 요구치, AP 충족.
     *
     * @return Usage 부족 상태 Arbitrary
     */
    @Provide
    Arbitrary<DefenseRankUpState> defenseUsageInsufficientState() {
        return nonMasterRank()
                .flatMap(
                        rank -> {
                            final RankUpRequirement requirement =
                                    skillRankPolicy.requirement(rank).orElseThrow();
                            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

                            final Arbitrary<Integer> usages =
                                    Arbitraries.integers()
                                            .between(0, requirement.requiredUsage() - 1);
                            final Arbitrary<Integer> aps =
                                    Arbitraries.integers().between(apCost, apCost + MAX_AP_SURPLUS);

                            return Combinators.combine(usages, aps)
                                    .as((usage, ap) -> new DefenseRankUpState(rank, usage, ap));
                        });
    }

    // ── Helpers ──

    private Arbitrary<SkillRank> nonMasterRank() {
        final SkillRank[] nonMasterRanks =
                java.util.Arrays.stream(SkillRank.values())
                        .filter(rank -> !rank.isMax())
                        .toArray(SkillRank[]::new);
        return Arbitraries.of(nonMasterRanks);
    }

    private DefenseSkill createDefenseSkillCatalog() {
        return new DefenseSkill(
                DEFENSE_SKILL_ID,
                "디펜스",
                SkillType.DEFENSE,
                SkillTalent.COMMON,
                4,
                Map.of(SkillRank.F, 50, SkillRank.E, 52),
                Map.of(SkillRank.F, 5, SkillRank.E, 5),
                "방어하며 반격한다.");
    }

    private CharacterProgress createProgressWithAp(final int abilityPoints) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        abilityPoints,
                        0L);
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
     * DEFENSE 스킬 랭크업 테스트용 상태 record.
     *
     * @param rank 스킬 랭크
     * @param usageCount 사용 횟수
     * @param abilityPoints 보유 AP
     */
    record DefenseRankUpState(SkillRank rank, int usageCount, int abilityPoints) {}
}
