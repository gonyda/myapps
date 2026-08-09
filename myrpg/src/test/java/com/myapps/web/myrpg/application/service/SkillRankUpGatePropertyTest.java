package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.RankUpRequirement;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillRankPolicy;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 랭크업 게이트 프로퍼티 테스트.
 *
 * <p>임의의 (rank, usageCount, killCount, abilityPoints) 조합에 대해
 * {@code rankUp}이 올바르게 게이트를 판정하는지 검증한다.
 * 성공 조건: usageCount ≥ required AND killCount ≥ required AND
 * abilityPoints ≥ apCost AND rank ≠ MASTER.
 * 그 외에는 상태가 불변이어야 한다.
 *
 * <p>Feature: 005-skill-system, Property 8: 랭크업 게이트
 *
 * <p><b>Validates: Requirements 5.3, 5.4, 5.5, 7.3</b>
 */
class SkillRankUpGatePropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String SKILL_ID = "windmill";
    private static final int MAX_USAGE_SURPLUS = 10;
    private static final int MAX_KILL_SURPLUS = 10;
    private static final int MAX_AP_SURPLUS = 10;

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * 모든 조건(사용·막타·AP)이 충족되고 MASTER가 아니면 랭크업이 성공한다.
     *
     * @param state 랭크업 가능한 상태(조건 충족 + AP 충족 + ≠MASTER)
     */
    @Property(tries = 100)
    void should_succeedRankUp_when_allConditionsMetAndNotMaster(
            @ForAll("rankableState") final RankUpTestState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, state.rank(), state.usageCount(), state.killCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockRepo.save(any(CharacterSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final boolean result = skillService.rankUp(progress, SKILL_ID);

        assertThat(result)
                .as("조건+AP 충족·≠MASTER이면 랭크업 성공")
                .isTrue();
    }

    /**
     * 사용 횟수가 부족하면 랭크업이 실패하고 상태가 불변이다.
     *
     * @param state 사용 횟수 부족 상태
     */
    @Property(tries = 100)
    void should_returnFalseAndKeepState_when_usageInsufficient(
            @ForAll("usageInsufficientState") final RankUpTestState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, state.rank(), state.usageCount(), state.killCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        final boolean result = skillService.rankUp(progress, SKILL_ID);

        assertThat(result)
                .as("사용 횟수 부족 시 랭크업 실패")
                .isFalse();
        assertStateUnchanged(skill, state, progress);
    }

    /**
     * 막타 처치 수가 부족하면 랭크업이 실패하고 상태가 불변이다.
     *
     * @param state 막타 처치 부족 상태
     */
    @Property(tries = 100)
    void should_returnFalseAndKeepState_when_killInsufficient(
            @ForAll("killInsufficientState") final RankUpTestState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, state.rank(), state.usageCount(), state.killCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        final boolean result = skillService.rankUp(progress, SKILL_ID);

        assertThat(result)
                .as("막타 처치 부족 시 랭크업 실패")
                .isFalse();
        assertStateUnchanged(skill, state, progress);
    }

    /**
     * 랭크가 MASTER이면 랭크업이 실패하고 상태가 불변이다.
     *
     * @param usageCount  임의의 사용 횟수
     * @param killCount   임의의 막타 처치 수
     * @param abilityPoints 임의의 AP
     */
    @Property(tries = 100)
    void should_returnFalseAndKeepState_when_rankIsMaster(
            @ForAll("usageForMaster") final int usageCount,
            @ForAll("killForMaster") final int killCount,
            @ForAll("apForMaster") final int abilityPoints) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, SkillRank.MASTER, usageCount, killCount);
        final CharacterProgress progress = createProgressWithAp(abilityPoints);

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        final boolean result = skillService.rankUp(progress, SKILL_ID);

        assertThat(result)
                .as("MASTER 랭크이면 랭크업 불가")
                .isFalse();
        assertThat(skill.getRank()).isEqualTo(SkillRank.MASTER);
        assertThat(skill.getUsageCount()).isEqualTo(usageCount);
        assertThat(skill.getKillCount()).isEqualTo(killCount);
        assertThat(progress.getAbilityPoints()).isEqualTo(abilityPoints);
    }

    /**
     * 조건은 충족했으나 AP가 부족하면 예외가 발생하고 상태가 불변이다.
     *
     * @param state AP 부족 상태(조건은 충족)
     */
    @Property(tries = 100)
    void should_throwExceptionAndKeepState_when_apInsufficient(
            @ForAll("apInsufficientState") final RankUpTestState state) {

        final CharacterSkillRepository mockRepo = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);
        final SkillService skillService = new SkillService(mockRepo, mock(CharacterProgressRepository.class), mockCatalog);

        final CharacterSkill skill = new CharacterSkill(
                CHARACTER_ID, SKILL_ID, state.rank(), state.usageCount(), state.killCount());
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());

        when(mockRepo.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));

        final Throwable thrown = catchThrowable(() -> skillService.rankUp(progress, SKILL_ID));

        assertThat(thrown)
                .as("조건 충족·AP 부족 시 InsufficientAbilityPointsException 발생")
                .isInstanceOf(InsufficientAbilityPointsException.class);
        assertStateUnchanged(skill, state, progress);
    }

    // ── Providers ──

    /**
     * 랭크업 가능 상태: ≠MASTER, 사용/막타 ≥ 요구치, AP ≥ apCost.
     *
     * @return Rankable 상태 Arbitrary
     */
    @Provide
    Arbitrary<RankUpTestState> rankableState() {
        return nonMasterRank().flatMap(rank -> {
            final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

            final Arbitrary<Integer> usages = Arbitraries.integers()
                    .between(requirement.requiredUsage(), requirement.requiredUsage() + MAX_USAGE_SURPLUS);
            final Arbitrary<Integer> kills = Arbitraries.integers()
                    .between(requirement.requiredKills(), requirement.requiredKills() + MAX_KILL_SURPLUS);
            final Arbitrary<Integer> aps = Arbitraries.integers()
                    .between(apCost, apCost + MAX_AP_SURPLUS);

            return Combinators.combine(usages, kills, aps)
                    .as((usage, kill, ap) -> new RankUpTestState(rank, usage, kill, ap));
        });
    }

    /**
     * 사용 횟수 부족 상태: ≠MASTER, 사용 < 요구치.
     *
     * @return 사용 부족 상태 Arbitrary
     */
    @Provide
    Arbitrary<RankUpTestState> usageInsufficientState() {
        return nonMasterRank().flatMap(rank -> {
            final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

            final Arbitrary<Integer> usages = Arbitraries.integers()
                    .between(0, requirement.requiredUsage() - 1);
            final Arbitrary<Integer> kills = Arbitraries.integers()
                    .between(0, requirement.requiredKills() + MAX_KILL_SURPLUS);
            final Arbitrary<Integer> aps = Arbitraries.integers()
                    .between(apCost, apCost + MAX_AP_SURPLUS);

            return Combinators.combine(usages, kills, aps)
                    .as((usage, kill, ap) -> new RankUpTestState(rank, usage, kill, ap));
        });
    }

    /**
     * 막타 처치 부족 상태: ≠MASTER, 막타 < 요구치, 사용 ≥ 요구치.
     *
     * @return 막타 부족 상태 Arbitrary
     */
    @Provide
    Arbitrary<RankUpTestState> killInsufficientState() {
        return nonMasterRank().flatMap(rank -> {
            final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

            final Arbitrary<Integer> usages = Arbitraries.integers()
                    .between(requirement.requiredUsage(), requirement.requiredUsage() + MAX_USAGE_SURPLUS);
            final Arbitrary<Integer> kills = Arbitraries.integers()
                    .between(0, requirement.requiredKills() - 1);
            final Arbitrary<Integer> aps = Arbitraries.integers()
                    .between(apCost, apCost + MAX_AP_SURPLUS);

            return Combinators.combine(usages, kills, aps)
                    .as((usage, kill, ap) -> new RankUpTestState(rank, usage, kill, ap));
        });
    }

    /**
     * AP 부족 상태: ≠MASTER, 사용/막타 ≥ 요구치, AP < apCost.
     *
     * @return AP 부족 상태 Arbitrary
     */
    @Provide
    Arbitrary<RankUpTestState> apInsufficientState() {
        return nonMasterRank().flatMap(rank -> {
            final RankUpRequirement requirement = skillRankPolicy.requirement(rank).orElseThrow();
            final int apCost = skillRankPolicy.apCost(rank).orElseThrow();

            final Arbitrary<Integer> usages = Arbitraries.integers()
                    .between(requirement.requiredUsage(), requirement.requiredUsage() + MAX_USAGE_SURPLUS);
            final Arbitrary<Integer> kills = Arbitraries.integers()
                    .between(requirement.requiredKills(), requirement.requiredKills() + MAX_KILL_SURPLUS);
            final Arbitrary<Integer> aps = Arbitraries.integers()
                    .between(0, apCost - 1);

            return Combinators.combine(usages, kills, aps)
                    .as((usage, kill, ap) -> new RankUpTestState(rank, usage, kill, ap));
        });
    }

    /**
     * MASTER 테스트용 임의 사용 횟수 (0~10000).
     *
     * @return 사용 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> usageForMaster() {
        return Arbitraries.integers().between(0, 10000);
    }

    /**
     * MASTER 테스트용 임의 막타 처치 수 (0~5000).
     *
     * @return 막타 처치 수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> killForMaster() {
        return Arbitraries.integers().between(0, 5000);
    }

    /**
     * MASTER 테스트용 임의 AP (0~300).
     *
     * @return AP Arbitrary
     */
    @Provide
    Arbitrary<Integer> apForMaster() {
        return Arbitraries.integers().between(0, 300);
    }

    // ── Helpers ──

    private Arbitrary<SkillRank> nonMasterRank() {
        final SkillRank[] nonMasterRanks = java.util.Arrays.stream(SkillRank.values())
                .filter(rank -> !rank.isMax())
                .toArray(SkillRank[]::new);
        return Arbitraries.of(nonMasterRanks);
    }

    private void assertStateUnchanged(final CharacterSkill skill,
                                      final RankUpTestState originalState,
                                      final CharacterProgress progress) {
        assertThat(skill.getRank())
                .as("실패 시 랭크 불변")
                .isEqualTo(originalState.rank());
        assertThat(skill.getUsageCount())
                .as("실패 시 사용 횟수 불변")
                .isEqualTo(originalState.usageCount());
        assertThat(skill.getKillCount())
                .as("실패 시 막타 처치 수 불변")
                .isEqualTo(originalState.killCount());
        assertThat(progress.getAbilityPoints())
                .as("실패 시 AP 불변")
                .isEqualTo(originalState.abilityPoints());
    }

    private CharacterProgress createProgressWithAp(final int abilityPoints) {
        final CharacterProgress progress = new CharacterProgress(
                "테스트", 1, 1, 0L,
                TalentType.MELEE,
                null, 100, 100, 100, "tir-chonaill", abilityPoints);
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
     * 랭크업 테스트용 상태 record.
     *
     * @param rank          스킬 랭크
     * @param usageCount    사용 횟수
     * @param killCount     막타 처치 수
     * @param abilityPoints 보유 AP
     */
    record RankUpTestState(SkillRank rank, int usageCount, int killCount, int abilityPoints) {
    }
}
