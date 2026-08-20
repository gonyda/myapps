package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.model.Bank;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.TalentType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * 은행 입출금 총량 보존 프로퍼티 테스트.
 *
 * <p>임의의 캐릭터 소지금(gold)과 은행 보관 골드(bankGold)에 대해, 성공한 입금/출금 후 {@code gold + bankGold} 총합이 항상 보존되고,
 * 소지금 초과 입금·은행 잔액 초과 출금은 {@link InsufficientGoldException}으로 거부되어 두 값이 모두 불변임을 검증한다.
 *
 * <p>BankService가 {@code ch.spendGold → bank.deposit} 및 {@code bank.withdraw → ch.gainGold}로 위임하는
 * 구조이므로, 순수 도메인 객체 호출로 동일 보존 속성을 직접 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 2: 은행 입출금 총량 보존
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b>
 */
class BankTransferPropertyTest {

    private static final long MAX_GOLD = 10_000L;
    private static final long MAX_BANK_GOLD = 10_000L;
    private static final long MAX_AMOUNT = 5_000L;

    // Feature: 006-gold-item-inventory, Property 2: 은행 입출금 총량 보존

    /**
     * 입금(amount ≤ gold) 성공 후 gold + bankGold 총합이 보존됨을 검증한다.
     *
     * @param tuple (initialGold, initialBankGold, depositAmount) 트리플
     */
    @Property(tries = 100)
    void should_preserveTotal_when_depositSucceeds(
            @ForAll("validDeposit") final Tuple.Tuple3<Long, Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long initialBankGold = tuple.get2();
        final long depositAmount = tuple.get3();
        final long originalTotal = initialGold + initialBankGold;

        final CharacterProgress ch = createProgressWithGold(initialGold);
        final Bank bank = createBankWithGold(initialBankGold);

        // BankService.deposit 흐름 시뮬레이션
        ch.spendGold(depositAmount);
        bank.deposit(depositAmount);

        assertThat(ch.getGold() + bank.getGold()).isEqualTo(originalTotal);
    }

    /**
     * 출금(amount ≤ bankGold) 성공 후 gold + bankGold 총합이 보존됨을 검증한다.
     *
     * @param tuple (initialGold, initialBankGold, withdrawAmount) 트리플
     */
    @Property(tries = 100)
    void should_preserveTotal_when_withdrawSucceeds(
            @ForAll("validWithdraw") final Tuple.Tuple3<Long, Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long initialBankGold = tuple.get2();
        final long withdrawAmount = tuple.get3();
        final long originalTotal = initialGold + initialBankGold;

        final CharacterProgress ch = createProgressWithGold(initialGold);
        final Bank bank = createBankWithGold(initialBankGold);

        // BankService.withdraw 흐름 시뮬레이션
        bank.withdraw(withdrawAmount);
        ch.gainGold(withdrawAmount);

        assertThat(ch.getGold() + bank.getGold()).isEqualTo(originalTotal);
    }

    /**
     * 입금 금액이 소지금을 초과하면 {@link InsufficientGoldException}이 발생하고 양쪽 모두 변하지 않음을 검증한다.
     *
     * @param tuple (initialGold, initialBankGold, depositAmount) 트리플: depositAmount > initialGold
     */
    @Property(tries = 100)
    void should_throwAndKeepBothUnchanged_when_depositExceedsGold(
            @ForAll("overflowDeposit") final Tuple.Tuple3<Long, Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long initialBankGold = tuple.get2();
        final long depositAmount = tuple.get3();

        final CharacterProgress ch = createProgressWithGold(initialGold);
        final Bank bank = createBankWithGold(initialBankGold);

        assertThatThrownBy(() -> ch.spendGold(depositAmount))
                .isInstanceOf(InsufficientGoldException.class);

        assertThat(ch.getGold()).isEqualTo(initialGold);
        assertThat(bank.getGold()).isEqualTo(initialBankGold);
    }

    /**
     * 출금 금액이 은행 잔액을 초과하면 {@link InsufficientGoldException}이 발생하고 양쪽 모두 변하지 않음을 검증한다.
     *
     * @param tuple (initialGold, initialBankGold, withdrawAmount) 트리플: withdrawAmount >
     *     initialBankGold
     */
    @Property(tries = 100)
    void should_throwAndKeepBothUnchanged_when_withdrawExceedsBankGold(
            @ForAll("overflowWithdraw") final Tuple.Tuple3<Long, Long, Long> tuple) {

        final long initialGold = tuple.get1();
        final long initialBankGold = tuple.get2();
        final long withdrawAmount = tuple.get3();

        final CharacterProgress ch = createProgressWithGold(initialGold);
        final Bank bank = createBankWithGold(initialBankGold);

        assertThatThrownBy(() -> bank.withdraw(withdrawAmount))
                .isInstanceOf(InsufficientGoldException.class);

        assertThat(ch.getGold()).isEqualTo(initialGold);
        assertThat(bank.getGold()).isEqualTo(initialBankGold);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 유효 입금 케이스를 생성하는 Arbitrary 제공자. initialGold ∈ [1, MAX_GOLD], initialBankGold ∈ [0,
     * MAX_BANK_GOLD], depositAmount ∈ [1, initialGold]
     *
     * @return (initialGold, initialBankGold, depositAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Long, Long, Long>> validDeposit() {
        return Arbitraries.longs()
                .between(1L, MAX_GOLD)
                .flatMap(
                        gold ->
                                Arbitraries.longs()
                                        .between(0L, MAX_BANK_GOLD)
                                        .flatMap(
                                                bankGold ->
                                                        Arbitraries.longs()
                                                                .between(1L, gold)
                                                                .map(
                                                                        amount ->
                                                                                Tuple.of(
                                                                                        gold,
                                                                                        bankGold,
                                                                                        amount))));
    }

    /**
     * 유효 출금 케이스를 생성하는 Arbitrary 제공자. initialGold ∈ [0, MAX_GOLD], initialBankGold ∈ [1,
     * MAX_BANK_GOLD], withdrawAmount ∈ [1, initialBankGold]
     *
     * @return (initialGold, initialBankGold, withdrawAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Long, Long, Long>> validWithdraw() {
        return Arbitraries.longs()
                .between(0L, MAX_GOLD)
                .flatMap(
                        gold ->
                                Arbitraries.longs()
                                        .between(1L, MAX_BANK_GOLD)
                                        .flatMap(
                                                bankGold ->
                                                        Arbitraries.longs()
                                                                .between(1L, bankGold)
                                                                .map(
                                                                        amount ->
                                                                                Tuple.of(
                                                                                        gold,
                                                                                        bankGold,
                                                                                        amount))));
    }

    /**
     * 입금 초과 케이스를 생성하는 Arbitrary 제공자. initialGold ∈ [0, MAX_AMOUNT], depositAmount ∈ [initialGold+1,
     * initialGold+MAX_AMOUNT]
     *
     * @return (initialGold, initialBankGold, depositAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Long, Long, Long>> overflowDeposit() {
        return Arbitraries.longs()
                .between(0L, MAX_AMOUNT)
                .flatMap(
                        gold ->
                                Arbitraries.longs()
                                        .between(0L, MAX_BANK_GOLD)
                                        .flatMap(
                                                bankGold ->
                                                        Arbitraries.longs()
                                                                .between(
                                                                        gold + 1L,
                                                                        gold + MAX_AMOUNT)
                                                                .map(
                                                                        amount ->
                                                                                Tuple.of(
                                                                                        gold,
                                                                                        bankGold,
                                                                                        amount))));
    }

    /**
     * 출금 초과 케이스를 생성하는 Arbitrary 제공자. initialBankGold ∈ [0, MAX_AMOUNT], withdrawAmount ∈
     * [initialBankGold+1, initialBankGold+MAX_AMOUNT]
     *
     * @return (initialGold, initialBankGold, withdrawAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Long, Long, Long>> overflowWithdraw() {
        return Arbitraries.longs()
                .between(0L, MAX_GOLD)
                .flatMap(
                        gold ->
                                Arbitraries.longs()
                                        .between(0L, MAX_AMOUNT)
                                        .flatMap(
                                                bankGold ->
                                                        Arbitraries.longs()
                                                                .between(
                                                                        bankGold + 1L,
                                                                        bankGold + MAX_AMOUNT)
                                                                .map(
                                                                        amount ->
                                                                                Tuple.of(
                                                                                        gold,
                                                                                        bankGold,
                                                                                        amount))));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * 지정된 골드 값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param gold 설정할 보유 골드
     * @return 해당 골드를 보유한 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithGold(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", 0, gold);
    }

    /**
     * 지정된 골드 값을 가진 {@link Bank}를 생성한다.
     *
     * <p>{@code Bank.createDefault()}로 골드 0 은행을 생성한 뒤, initialBankGold가 0보다 크면 deposit으로 잔액을 설정한다.
     *
     * @param initialBankGold 설정할 은행 보관 골드
     * @return 해당 골드를 보관한 Bank 인스턴스
     */
    private Bank createBankWithGold(final long initialBankGold) {
        final Bank bank = Bank.createDefault();
        if (initialBankGold > 0) {
            bank.deposit(initialBankGold);
        }
        return bank;
    }
}
