package com.myapps.web.myrpg.domain.model;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 골드 증감 불변식 프로퍼티 테스트.
 *
 * <p>임의의 초기 골드와 gain/spend 시퀀스에 대해, {@code gainGold}/{@code spendGold} 후
 * 소지금이 항상 0 이상이며, 소모 금액이 보유량을 초과하면
 * {@link InsufficientGoldException}을 던지고 소지금을 변경하지 않음을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 1: 골드 증감 불변식
 *
 * <p><b>Validates: Requirements 1.3, 1.4, 1.5</b>
 */
class GoldGainSpendPropertyTest {

    private static final long MAX_GOLD = 10_000L;
    private static final long MAX_AMOUNT = 5_000L;

    /**
     * gainGold 후 소지금이 항상 0 이상이고, 증가량이 정확히 반영됨을 검증한다.
     *
     * @param tuple (initialGold, gainAmount) 쌍
     */
    @Property(tries = 100)
    void should_keepGoldNonNegative_when_gainGold(
            @ForAll("validGain") final Tuple.Tuple2<Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long gainAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithGold(initialGold);

        progress.gainGold(gainAmount);

        assertThat(progress.getGold()).isEqualTo(initialGold + gainAmount);
        assertThat(progress.getGold()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * spendGold(amount <= gold) 후 소지금이 항상 0 이상이고, 차감량이 정확히 반영됨을 검증한다.
     *
     * @param tuple (initialGold, spendAmount) 쌍: spendAmount ∈ [1, initialGold]
     */
    @Property(tries = 100)
    void should_keepGoldNonNegative_when_spendWithinBalance(
            @ForAll("validSpend") final Tuple.Tuple2<Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long spendAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithGold(initialGold);

        progress.spendGold(spendAmount);

        assertThat(progress.getGold()).isEqualTo(initialGold - spendAmount);
        assertThat(progress.getGold()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * spendGold(amount > gold) 시 {@link InsufficientGoldException}이 발생하고
     * 소지금이 변하지 않음을 검증한다.
     *
     * @param tuple (initialGold, spendAmount) 쌍: spendAmount ∈ [initialGold+1, initialGold+5000]
     */
    @Property(tries = 100)
    void should_throwAndKeepGoldUnchanged_when_spendExceedsBalance(
            @ForAll("overflowSpend") final Tuple.Tuple2<Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long spendAmount = tuple.get2();

        final CharacterProgress progress = createProgressWithGold(initialGold);

        assertThatThrownBy(() -> progress.spendGold(spendAmount))
                .isInstanceOf(InsufficientGoldException.class);

        assertThat(progress.getGold()).isEqualTo(initialGold);
    }

    /**
     * 초기 골드 [0, 10000] 범위에서 선택되고, 획득량이 [1, 5000] 범위인
     * 유효 골드 획득 케이스를 생성하는 Arbitrary 제공자.
     *
     * @return (initialGold, gainAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Long>> validGain() {
        return Arbitraries.longs().between(0L, MAX_GOLD)
                .flatMap(gold -> Arbitraries.longs().between(1L, MAX_AMOUNT)
                        .map(gain -> Tuple.of(gold, gain)));
    }

    /**
     * 초기 골드 [1, 10000] 범위에서 선택되고, 소모량이 [1, initialGold] 범위인
     * 유효 골드 소모 케이스를 생성하는 Arbitrary 제공자.
     *
     * @return (initialGold, spendAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Long>> validSpend() {
        return Arbitraries.longs().between(1L, MAX_GOLD)
                .flatMap(gold -> Arbitraries.longs().between(1L, gold)
                        .map(spend -> Tuple.of(gold, spend)));
    }

    /**
     * 초기 골드 [0, 5000] 범위에서 선택되고, 소모량이 [initialGold+1, initialGold+5000] 범위인
     * 초과 소모 케이스를 생성하는 Arbitrary 제공자.
     *
     * @return (initialGold, spendAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Long>> overflowSpend() {
        return Arbitraries.longs().between(0L, MAX_AMOUNT)
                .flatMap(gold -> Arbitraries.longs().between(gold + 1L, gold + MAX_AMOUNT)
                        .map(spend -> Tuple.of(gold, spend)));
    }

    /**
     * 지정된 골드 값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param gold 설정할 보유 골드
     * @return 해당 골드를 보유한 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithGold(final long gold) {
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
                0,
                gold
        );
    }
}
