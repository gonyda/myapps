package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;

/**
 * HP 감소·사망 전이 프로퍼티 테스트.
 *
 * <p>임의의 HP 현재값과 피해량에 대해, {@code damageHp(amount)} 호출 결과가 {@code max(0, hpCurrent − amount)}와 정확히
 * 일치하고, {@code isDead()}가 {@code hpCurrent == 0}과 동치임을 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 11: HP 감소·사망 전이
 *
 * <p><b>Validates: Requirements 11.1, 11.2</b>
 */
// Feature: 008-battle-system, Property 11: HP 감소·사망 전이
class CharacterProgressDamageHpPropertyTest {

    private static final int MAX_HP = 10_000;
    private static final int MAX_DAMAGE = 15_000;

    /**
     * damageHp(amount) 후 hpCurrent가 max(0, originalHp - amount)와 같음을 검증한다.
     *
     * @param tuple (initialHp, damageAmount) 쌍
     */
    @Property(tries = 100)
    void should_floorHpAtZero_when_damageApplied(
            @ForAll("hpAndDamage") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int initialHp = tuple.get1();
        final int damageAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithHp(initialHp);

        progress.damageHp(damageAmount);

        final int expectedHp = Math.max(0, initialHp - damageAmount);
        assertThat(progress.getHpCurrent()).isEqualTo(expectedHp);
    }

    /**
     * isDead()가 hpCurrent == 0일 때만 true를 반환함을 검증한다.
     *
     * @param tuple (initialHp, damageAmount) 쌍
     */
    @Property(tries = 100)
    void should_returnIsDeadTrue_whenAndOnlyWhen_hpIsZero(
            @ForAll("hpAndDamage") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int initialHp = tuple.get1();
        final int damageAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithHp(initialHp);

        progress.damageHp(damageAmount);

        final boolean expectedDead = progress.getHpCurrent() == 0;
        assertThat(progress.isDead()).isEqualTo(expectedDead);
    }

    /**
     * damageHp(0)은 HP를 변경하지 않음을 검증한다.
     *
     * @param initialHp 임의의 초기 HP (1 이상)
     */
    @Property(tries = 100)
    void should_leaveHpUnchanged_when_damageIsZero(
            @ForAll @IntRange(min = 1, max = MAX_HP) final int initialHp) {

        final CharacterProgress progress = createProgressWithHp(initialHp);

        progress.damageHp(0);

        assertThat(progress.getHpCurrent()).isEqualTo(initialHp);
        assertThat(progress.isDead()).isFalse();
    }

    /**
     * damageHp(amount > hpCurrent)이 HP를 정확히 0으로 설정함을 검증한다.
     *
     * @param tuple (initialHp, excessDamage) 쌍: excessDamage > initialHp
     */
    @Property(tries = 100)
    void should_setHpToZero_when_damageExceedsCurrentHp(
            @ForAll("excessDamage") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int initialHp = tuple.get1();
        final int damageAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithHp(initialHp);

        progress.damageHp(damageAmount);

        assertThat(progress.getHpCurrent()).isZero();
        assertThat(progress.isDead()).isTrue();
    }

    /**
     * damageHp 후 HP가 0보다 크면 isDead()가 false임을 검증한다.
     *
     * @param tuple (initialHp, partialDamage) 쌍: partialDamage < initialHp
     */
    @Property(tries = 100)
    void should_notBeDead_when_hpRemainsAboveZero(
            @ForAll("partialDamage") final Tuple.Tuple2<Integer, Integer> tuple) {

        final int initialHp = tuple.get1();
        final int damageAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithHp(initialHp);

        progress.damageHp(damageAmount);

        assertThat(progress.getHpCurrent()).isGreaterThan(0);
        assertThat(progress.isDead()).isFalse();
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 초기 HP [1, 10000] 범위와 피해량 [0, 15000] 범위를 조합한 튜플을 생성한다.
     *
     * @return (initialHp, damageAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Integer>> hpAndDamage() {
        return Arbitraries.integers()
                .between(1, MAX_HP)
                .flatMap(
                        hp ->
                                Arbitraries.integers()
                                        .between(0, MAX_DAMAGE)
                                        .map(damage -> Tuple.of(hp, damage)));
    }

    /**
     * 초기 HP [1, 10000] 범위에서 선택되고, 피해량이 (initialHp, 15000] 범위인 초과 피해 케이스를 생성한다.
     *
     * @return (initialHp, excessDamage) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Integer>> excessDamage() {
        return Arbitraries.integers()
                .between(1, MAX_HP)
                .flatMap(
                        hp ->
                                Arbitraries.integers()
                                        .between(hp + 1, MAX_DAMAGE + 1)
                                        .map(damage -> Tuple.of(hp, damage)));
    }

    /**
     * 초기 HP [2, 10000] 범위에서 선택되고, 피해량이 [1, initialHp-1] 범위인 부분 피해 케이스를 생성한다.
     *
     * @return (initialHp, partialDamage) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Integer>> partialDamage() {
        return Arbitraries.integers()
                .between(2, MAX_HP)
                .flatMap(
                        hp ->
                                Arbitraries.integers()
                                        .between(1, hp - 1)
                                        .map(damage -> Tuple.of(hp, damage)));
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    /**
     * 지정된 HP 현재값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param hpCurrent 설정할 HP 현재값
     * @return 해당 HP를 가진 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithHp(final int hpCurrent) {
        return new CharacterProgress(
                "테스트",
                1,
                1,
                0L,
                TalentType.MELEE,
                null,
                hpCurrent,
                100,
                100,
                "tir-chonaill",
                0,
                0L);
    }
}
