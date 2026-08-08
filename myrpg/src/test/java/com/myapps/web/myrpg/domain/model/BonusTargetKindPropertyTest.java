package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대상 종류 분류가 정확하고, {@link StatProgression}이 STAT 보너스를 바이탈에,
 * VITAL 보너스를 스탯에 가산하지 않음을 검증하는 프로퍼티 테스트.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 8: 대상 종류 분류
 *
 * <p><b>Validates: Requirements 7.6, 11.2</b>
 */
class BonusTargetKindPropertyTest {

    private static final Set<BonusTarget> STAT_TARGETS = Set.of(
            BonusTarget.STR, BonusTarget.DEX, BonusTarget.INT, BonusTarget.CRITICAL);

    private static final Set<BonusTarget> VITAL_TARGETS = Set.of(
            BonusTarget.HP, BonusTarget.MP, BonusTarget.STAMINA);

    private static final int COMMON_VITAL_BASE = 100;
    private static final int COMMON_VITAL_PER_LEVEL = 10;

    private final StatProgression statProgression = new StatProgression();

    /**
     * 모든 {@link BonusTarget} 값에 대해, STR/DEX/INT/CRITICAL의 {@code kind()}는
     * {@link BonusKind#STAT}이고, HP/MP/STAMINA의 {@code kind()}는
     * {@link BonusKind#VITAL}인지 검증한다.
     *
     * @param target 임의의 BonusTarget 열거 상수
     */
    @Property(tries = 100)
    void should_classifyKindCorrectly_forAllBonusTargets(@ForAll("bonusTargets") final BonusTarget target) {
        if (STAT_TARGETS.contains(target)) {
            assertThat(target.kind()).isEqualTo(BonusKind.STAT);
        } else {
            assertThat(target.kind()).isEqualTo(BonusKind.VITAL);
            assertThat(VITAL_TARGETS).contains(target);
        }
    }

    /**
     * 임의의 레벨 L과 재능 T에 대해, {@code levelStatsFor(L, T)}와 공통 스탯의 차이가
     * 오직 재능의 {@link BonusKind#STAT} 대상에서만 발생하는지 검증한다.
     *
     * <p>VITAL 계열 보너스(HP/MP/STAMINA)는 스탯 차이를 만들지 않아야 한다.
     *
     * @param level  1 이상 100 이하의 임의 레벨
     * @param talent 임의의 재능 유형
     */
    @Property(tries = 100)
    void should_notApplyVitalBonusToStats(@ForAll("levels") final int level,
                                          @ForAll("talents") final TalentType talent) {
        final Stats talentStats = statProgression.levelStatsFor(level, talent);
        final Stats common = statProgression.levelStatsFor(level);

        // STR 차이가 있으면, 재능의 주/보조 보너스 중 하나가 STR(STAT 계열)을 대상으로 해야 한다
        if (talentStats.str() != common.str()) {
            assertThat(hasStatBonusTargeting(talent, BonusTarget.STR)).isTrue();
        }
        if (talentStats.dex() != common.dex()) {
            assertThat(hasStatBonusTargeting(talent, BonusTarget.DEX)).isTrue();
        }
        if (talentStats.intelligence() != common.intelligence()) {
            assertThat(hasStatBonusTargeting(talent, BonusTarget.INT)).isTrue();
        }
        if (talentStats.critical() != common.critical()) {
            assertThat(hasStatBonusTargeting(talent, BonusTarget.CRITICAL)).isTrue();
        }
        // DEF는 재능 보너스 대상이 아니므로 항상 공통값과 동일
        assertThat(talentStats.defense()).isEqualTo(common.defense());
    }

    /**
     * 임의의 레벨 L과 재능 T에 대해, {@code vitalMaxFor(L, T)}와 공통 바이탈의 차이가
     * 오직 재능의 {@link BonusKind#VITAL} 대상에서만 발생하는지 검증한다.
     *
     * <p>STAT 계열 보너스(STR/DEX/INT/CRITICAL)는 바이탈 차이를 만들지 않아야 한다.
     *
     * @param level  1 이상 100 이하의 임의 레벨
     * @param talent 임의의 재능 유형
     */
    @Property(tries = 100)
    void should_notApplyStatBonusToVitals(@ForAll("levels") final int level,
                                          @ForAll("talents") final TalentType talent) {
        final VitalMax talentVital = statProgression.vitalMaxFor(level, talent);
        final int commonVital = COMMON_VITAL_BASE + COMMON_VITAL_PER_LEVEL * (level - 1);

        // HP 차이가 있으면, 재능의 주/보조 보너스 중 하나가 HP(VITAL 계열)를 대상으로 해야 한다
        if (talentVital.hp() != commonVital) {
            assertThat(hasVitalBonusTargeting(talent, BonusTarget.HP)).isTrue();
        }
        if (talentVital.mp() != commonVital) {
            assertThat(hasVitalBonusTargeting(talent, BonusTarget.MP)).isTrue();
        }
        if (talentVital.stamina() != commonVital) {
            assertThat(hasVitalBonusTargeting(talent, BonusTarget.STAMINA)).isTrue();
        }
    }

    /**
     * 해당 재능이 주어진 스탯 대상을 STAT 계열 보너스로 갖고 있는지 확인한다.
     *
     * @param talent 재능 유형
     * @param target 확인할 보너스 대상
     * @return 재능의 주/보조 보너스 중 하나가 해당 STAT 대상이면 {@code true}
     */
    private boolean hasStatBonusTargeting(final TalentType talent, final BonusTarget target) {
        return (talent.primary().target() == target && talent.primary().target().kind() == BonusKind.STAT)
                || (talent.secondary().target() == target && talent.secondary().target().kind() == BonusKind.STAT);
    }

    /**
     * 해당 재능이 주어진 바이탈 대상을 VITAL 계열 보너스로 갖고 있는지 확인한다.
     *
     * @param talent 재능 유형
     * @param target 확인할 보너스 대상
     * @return 재능의 주/보조 보너스 중 하나가 해당 VITAL 대상이면 {@code true}
     */
    private boolean hasVitalBonusTargeting(final TalentType talent, final BonusTarget target) {
        return (talent.primary().target() == target && talent.primary().target().kind() == BonusKind.VITAL)
                || (talent.secondary().target() == target && talent.secondary().target().kind() == BonusKind.VITAL);
    }

    /**
     * 유효한 레벨(1~100)을 생성하는 Arbitrary 제공자.
     *
     * @return 1 이상 100 이하의 정수를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 모든 {@link TalentType} 상수를 균등하게 선택하는 Arbitrary 제공자.
     *
     * @return TalentType 3종 중 하나를 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 모든 {@link BonusTarget} 상수를 균등하게 선택하는 Arbitrary 제공자.
     *
     * @return BonusTarget 7종 중 하나를 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<BonusTarget> bonusTargets() {
        return Arbitraries.of(BonusTarget.values());
    }
}
