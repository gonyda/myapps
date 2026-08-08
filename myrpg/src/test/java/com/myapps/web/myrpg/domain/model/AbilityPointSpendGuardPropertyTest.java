package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AP 소모 가드 프로퍼티 테스트.
 *
 * <p>{@code c ≤ 보유}면 {@code spendAbilityPoints(c)}로 {@code c}만큼 감소하고,
 * {@code c > 보유}면 예외가 발생하며 음수가 되지 않음을 검증한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 4: AP 소모 가드
 *
 * <p><b>Validates: Requirements 2.3, 2.4</b>
 */
class AbilityPointSpendGuardPropertyTest {

    private static final int MAX_AP = 200;
    private static final int OVERFLOW_RANGE = 100;

    /**
     * 소모량이 보유 AP 이하일 때, {@code spendAbilityPoints(spendAmount)} 호출 후
     * 보유 AP가 정확히 {@code spendAmount}만큼 감소하고 음수가 아님을 검증한다.
     *
     * @param tuple (abilityPoints, spendAmount) 쌍: spendAmount ∈ [0, abilityPoints]
     */
    @Property(tries = 100)
    void should_decreaseAP_when_spendAmountIsWithinBalance(
            @ForAll("validSpend") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int abilityPoints = tuple.get1();
        final int spendAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithAP(abilityPoints);
        final int originalAP = progress.getAbilityPoints();

        progress.spendAbilityPoints(spendAmount);

        assertThat(progress.getAbilityPoints()).isEqualTo(originalAP - spendAmount);
        assertThat(progress.getAbilityPoints()).isGreaterThanOrEqualTo(0);
    }

    /**
     * 소모량이 보유 AP를 초과할 때, {@code spendAbilityPoints(spendAmount)} 호출 시
     * {@link IllegalArgumentException}이 발생하고, 보유 AP가 변하지 않음을 검증한다.
     *
     * @param tuple (abilityPoints, spendAmount) 쌍: spendAmount ∈ [abilityPoints+1, abilityPoints+100]
     */
    @Property(tries = 100)
    void should_throwException_when_spendAmountExceedsBalance(
            @ForAll("overflowSpend") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int abilityPoints = tuple.get1();
        final int spendAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithAP(abilityPoints);
        final int originalAP = progress.getAbilityPoints();

        assertThatThrownBy(() -> progress.spendAbilityPoints(spendAmount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(progress.getAbilityPoints()).isEqualTo(originalAP);
    }

    /**
     * AP가 [0, 200] 범위에서 선택되고, 소모량이 [0, abilityPoints] 범위인
     * 유효 소모 케이스를 생성하는 Arbitrary 제공자.
     *
     * @return (abilityPoints, spendAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Integer>> validSpend() {
        return Arbitraries.integers().between(0, MAX_AP)
                .flatMap(ap -> Arbitraries.integers().between(0, ap)
                        .map(spend -> Tuple.of(ap, spend)));
    }

    /**
     * AP가 [0, 100] 범위에서 선택되고, 소모량이 [abilityPoints+1, abilityPoints+100] 범위인
     * 초과 소모 케이스를 생성하는 Arbitrary 제공자.
     *
     * @return (abilityPoints, spendAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Integer>> overflowSpend() {
        return Arbitraries.integers().between(0, OVERFLOW_RANGE)
                .flatMap(ap -> Arbitraries.integers().between(ap + 1, ap + OVERFLOW_RANGE)
                        .map(spend -> Tuple.of(ap, spend)));
    }

    /**
     * 지정된 AP 값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param abilityPoints 설정할 보유 AP
     * @return 해당 AP를 보유한 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithAP(final int abilityPoints) {
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
                abilityPoints
        );
    }
}
