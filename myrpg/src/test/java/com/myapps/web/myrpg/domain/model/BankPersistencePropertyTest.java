package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.repository.BankRepository;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

/**
 * Bank 엔티티의 영속 라운드트립 및 단일 행 보장 프로퍼티 테스트.
 *
 * <p>저장 후 조회 시 {@code gold} 값이 보존되며, {@code findFirstByOrderByIdAsc}가 항상 동일 행을 반환하여 싱글 플레이어 은행의 단일
 * 행 불변식을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 17: 영속 라운드트립
 *
 * <p><b>Validates: Requirements 2.1, 2.2</b>
 */
@JqwikSpringSupport
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BankPersistencePropertyTest {

    private static final long MAX_GOLD = 100_000L;

    private final TestEntityManager entityManager;
    private final BankRepository bankRepository;

    BankPersistencePropertyTest(
            final TestEntityManager entityManager, final BankRepository bankRepository) {
        this.entityManager = entityManager;
        this.bankRepository = bankRepository;
    }

    // Feature: 006-gold-item-inventory, Property 17: 영속 라운드트립

    /**
     * 임의의 gold 값으로 Bank를 저장 후 findById로 조회하면 gold가 정확히 보존됨을 검증한다.
     *
     * @param initialGold 초기 은행 골드 값
     */
    @Property(tries = 100)
    void should_preserveGold_when_savedAndFoundById(@ForAll("goldValues") final long initialGold) {

        final Bank bank = Bank.createDefault();
        if (initialGold > 0) {
            bank.deposit(initialGold);
        }

        entityManager.persistAndFlush(bank);
        final Long savedId = bank.getId();
        entityManager.clear();

        final Optional<Bank> found = bankRepository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getGold()).isEqualTo(initialGold);
    }

    /**
     * deposit/withdraw 후 flush·clear하고 다시 조회하면 변경된 gold가 정확히 반영됨을 검증한다.
     *
     * @param initialGold 초기 은행 골드 (1 이상, deposit 후 withdraw 가능하도록)
     * @param withdrawAmount 출금할 금액 (1 이상, initialGold 이하)
     */
    @Property(tries = 100)
    void should_preserveGoldAfterWithdraw_when_savedAndReloaded(
            @ForAll("validWithdrawPair") final net.jqwik.api.Tuple.Tuple2<Long, Long> pair) {

        final long initialGold = pair.get1();
        final long withdrawAmount = pair.get2();

        final Bank bank = Bank.createDefault();
        bank.deposit(initialGold);
        entityManager.persistAndFlush(bank);
        final Long savedId = bank.getId();

        bank.withdraw(withdrawAmount);
        entityManager.flush();
        entityManager.clear();

        final Optional<Bank> found = bankRepository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getGold()).isEqualTo(initialGold - withdrawAmount);
    }

    /**
     * findFirstByOrderByIdAsc가 저장된 행이 없을 때 빈 Optional을 반환하고, 한 행 저장 후에는 해당 행을 반환함을 검증한다
     * (loadOrCreateDefault 선례).
     *
     * @param initialGold 초기 은행 골드 값
     */
    @Property(tries = 100)
    void should_returnSingleRow_when_findFirstByOrderByIdAsc(
            @ForAll("goldValues") final long initialGold) {

        // 저장된 행이 없을 때 빈 Optional
        final Optional<Bank> beforeSave = bankRepository.findFirstByOrderByIdAsc();
        assertThat(beforeSave).isEmpty();

        // 행 1개 생성 후 조회
        final Bank bank = Bank.createDefault();
        if (initialGold > 0) {
            bank.deposit(initialGold);
        }
        entityManager.persistAndFlush(bank);
        entityManager.clear();

        final Optional<Bank> afterSave = bankRepository.findFirstByOrderByIdAsc();

        assertThat(afterSave).isPresent();
        assertThat(afterSave.get().getGold()).isEqualTo(initialGold);
    }

    /**
     * 여러 행이 저장되어도 findFirstByOrderByIdAsc는 항상 id가 가장 작은 행을 반환함을 검증한다. 이는 싱글 플레이어에서 은행 행이 유일함을 보장하는
     * 쿼리 동작이다.
     *
     * @param firstGold 첫 번째 은행 행의 골드
     * @param secondGold 두 번째 은행 행의 골드
     */
    @Property(tries = 100)
    void should_returnFirstRow_when_multipleRowsExist(
            @ForAll("goldValues") final long firstGold,
            @ForAll("goldValues") final long secondGold) {

        final Bank first = Bank.createDefault();
        if (firstGold > 0) {
            first.deposit(firstGold);
        }
        entityManager.persistAndFlush(first);

        final Bank second = Bank.createDefault();
        if (secondGold > 0) {
            second.deposit(secondGold);
        }
        entityManager.persistAndFlush(second);
        entityManager.clear();

        final Optional<Bank> found = bankRepository.findFirstByOrderByIdAsc();

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(first.getId());
        assertThat(found.get().getGold()).isEqualTo(firstGold);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 은행 골드 값 Arbitrary를 제공한다 (0~100,000).
     *
     * @return 은행 골드 Arbitrary
     */
    @Provide
    Arbitrary<Long> goldValues() {
        return Arbitraries.longs().between(0L, MAX_GOLD);
    }

    /**
     * 유효 출금 쌍을 생성하는 Arbitrary 제공자. initialGold ∈ [1, MAX_GOLD], withdrawAmount ∈ [1, initialGold]
     *
     * @return (initialGold, withdrawAmount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<net.jqwik.api.Tuple.Tuple2<Long, Long>> validWithdrawPair() {
        return Arbitraries.longs()
                .between(1L, MAX_GOLD)
                .flatMap(
                        gold ->
                                Arbitraries.longs()
                                        .between(1L, gold)
                                        .map(amount -> net.jqwik.api.Tuple.of(gold, amount)));
    }
}
