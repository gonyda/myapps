package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
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

/**
 * AP 소모 가드 프로퍼티 테스트.
 *
 * <p>조건(사용 횟수·막타 처치)은 충족되었으나 보유 AP가 소모 비용 미만인 상태에서 랭크업을 시도하면 {@link
 * InsufficientAbilityPointsException}이 발생하고, 캐릭터 상태(랭크·사용 횟수·막타 처치·AP)가 일절 변경되지 않으며 AP가 음수가 되지 않음을
 * 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 10: AP 소모 가드
 *
 * <p><b>Validates: Requirements 6.4, 6.5</b>
 */
class SkillRankUpApGuardPropertyTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String SKILL_ID = "windmill";

    private final SkillRankPolicy skillRankPolicy = new SkillRankPolicy();

    /**
     * AP가 소모 비용 미만인 상태에서 랭크업 시도 시 {@link InsufficientAbilityPointsException}이 발생하고 모든 상태가 불변임을
     * 검증한다.
     *
     * @param state 조건 충족 + AP 부족 상태
     */
    @Property(tries = 100)
    void should_throwException_and_preserveState_when_apIsInsufficient(
            @ForAll("apInsufficientStates") final ApInsufficientState state) {

        // Given: mock repository
        final CharacterSkillRepository mockRepository = mock(CharacterSkillRepository.class);
        final SkillCatalogService mockCatalog = mock(SkillCatalogService.class);

        final CharacterSkill skill =
                new CharacterSkill(
                        CHARACTER_ID,
                        SKILL_ID,
                        state.rank(),
                        state.usageCount(),
                        state.killCount());
        when(mockRepository.findByCharacterIdAndSkillId(CHARACTER_ID, SKILL_ID))
                .thenReturn(Optional.of(skill));
        when(mockCatalog.byId(SKILL_ID))
                .thenReturn(
                        Optional.of(
                                new DamageSkill(
                                        SKILL_ID,
                                        "윈드밀",
                                        SkillType.NORMAL,
                                        SkillTalent.MELEE,
                                        7,
                                        Map.of(SkillRank.F, 35, SkillRank.E, 38),
                                        "범위 공격")));

        final SkillService skillService =
                new SkillService(
                        mockRepository, mock(CharacterProgressRepository.class), mockCatalog);
        final CharacterProgress progress = createProgressWithAp(state.abilityPoints());
        setId(progress, CHARACTER_ID);

        // 상태 스냅샷
        final SkillRank originalRank = skill.getRank();
        final int originalUsage = skill.getUsageCount();
        final int originalKill = skill.getKillCount();
        final int originalAp = progress.getAbilityPoints();

        // When & Then: 예외 발생
        assertThatThrownBy(() -> skillService.rankUp(progress, SKILL_ID))
                .isInstanceOf(InsufficientAbilityPointsException.class);

        // Then: 상태 불변
        assertThat(skill.getRank()).as("랭크가 변경되지 않아야 한다").isEqualTo(originalRank);
        assertThat(skill.getUsageCount()).as("사용 횟수가 변경되지 않아야 한다").isEqualTo(originalUsage);
        assertThat(skill.getKillCount()).as("막타 처치 수가 변경되지 않아야 한다").isEqualTo(originalKill);
        assertThat(progress.getAbilityPoints()).as("AP가 변경되지 않아야 한다").isEqualTo(originalAp);

        // Then: AP가 음수가 되지 않음
        assertThat(progress.getAbilityPoints()).as("AP는 음수가 될 수 없다").isGreaterThanOrEqualTo(0);

        // Then: 저장이 호출되지 않음
        verify(mockRepository, never()).save(skill);
    }

    /**
     * 조건 충족 + AP 부족 상태를 생성한다.
     *
     * <p>MASTER를 제외한 모든 랭크에 대해, 사용 횟수·막타 처치는 요구치 이상이되 AP는 해당 랭크의 소모 비용 미만(0 ~ apCost-1)으로 생성한다.
     *
     * @return AP 부족 상태 Arbitrary
     */
    @Provide
    Arbitrary<ApInsufficientState> apInsufficientStates() {
        // MASTER를 제외한 랭크
        final Arbitrary<SkillRank> ranks =
                Arbitraries.of(
                        SkillRank.F,
                        SkillRank.E,
                        SkillRank.D,
                        SkillRank.C,
                        SkillRank.B,
                        SkillRank.A,
                        SkillRank.R9,
                        SkillRank.R8,
                        SkillRank.R7,
                        SkillRank.R6,
                        SkillRank.R5,
                        SkillRank.R4,
                        SkillRank.R3,
                        SkillRank.R2,
                        SkillRank.R1);

        return ranks.flatMap(
                rank -> {
                    final int apCost = skillRankPolicy.apCost(rank).orElseThrow();
                    final int reqUsage =
                            skillRankPolicy.requirement(rank).orElseThrow().requiredUsage();
                    final int reqKills =
                            skillRankPolicy.requirement(rank).orElseThrow().requiredKills();

                    // AP: 0 ~ apCost - 1 (부족 보장)
                    final Arbitrary<Integer> ap = Arbitraries.integers().between(0, apCost - 1);

                    // 사용 횟수: 요구치 이상 (요구치 ~ 요구치 * 2)
                    final Arbitrary<Integer> usage =
                            Arbitraries.integers().between(reqUsage, reqUsage * 2);

                    // 막타 처치: 요구치 이상 (요구치 ~ 요구치 * 2)
                    final Arbitrary<Integer> kills =
                            Arbitraries.integers().between(reqKills, reqKills * 2);

                    return Combinators.combine(ap, usage, kills)
                            .as(
                                    (apVal, usageVal, killVal) ->
                                            new ApInsufficientState(
                                                    rank, usageVal, killVal, apVal));
                });
    }

    private CharacterProgress createProgressWithAp(final int abilityPoints) {
        return new CharacterProgress(
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
     * 조건 충족 + AP 부족 상태를 캡슐화하는 레코드.
     *
     * @param rank 현재 스킬 랭크 (MASTER 제외)
     * @param usageCount 사용 횟수 (요구치 이상)
     * @param killCount 막타 처치 수 (요구치 이상)
     * @param abilityPoints 보유 AP (소모 비용 미만)
     */
    record ApInsufficientState(SkillRank rank, int usageCount, int killCount, int abilityPoints) {}
}
