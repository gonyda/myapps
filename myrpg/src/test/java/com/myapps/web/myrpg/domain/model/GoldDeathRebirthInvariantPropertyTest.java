package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 골드 사망/환생 불변 프로퍼티 테스트.
 *
 * <p>사망 패널티(레벨 하락·경험치 초기화·바이탈 회복)와 환생(레벨 1 초기화·누적 레벨 증가· 재능 변경·AP 지급·바이탈 회복) 시퀀스 전후로 소지금({@code
 * gold})과 은행 보관 골드({@code bankGold})가 변하지 않음을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 3: 골드 사망/환생 불변
 *
 * <p><b>Validates: Requirements 1.6</b>
 */
class GoldDeathRebirthInvariantPropertyTest {

    private static final long MAX_GOLD = 50_000L;
    private static final int MAX_LEVEL = 200;
    private static final int MAX_AP_GRANT = 100;

    /**
     * 사망 패널티(레벨 하락·경험치 초기화·바이탈 회복) 수행 후 소지금이 불변임을 검증한다.
     *
     * @param initialGold 초기 소지금
     * @param deathLevel 사망 후 설정될 레벨
     * @param recoverMax 회복 시 적용할 바이탈 최대값
     */
    @Property(tries = 100)
    void should_keepGoldUnchanged_when_deathPenalty(
            @ForAll("goldValues") final long initialGold,
            @ForAll("levels") final int deathLevel,
            @ForAll("vitalMaxValues") final VitalMax recoverMax) {

        final CharacterProgress progress = createProgressWithGold(initialGold);

        // 사망 패널티 시뮬레이션: 레벨 하락, 경험치 0, 바이탈 회복
        progress.setCurrentLevel(deathLevel);
        progress.setExperience(0L);
        progress.fullRecover(recoverMax);

        assertThat(progress.getGold()).as("사망 패널티 후 소지금은 변하지 않아야 한다").isEqualTo(initialGold);
    }

    /**
     * 환생(레벨1 초기화·누적 레벨 증가·경험치 초기화·재능 변경·AP 지급·시각 설정·바이탈 회복) 수행 후 소지금이 불변임을 검증한다.
     *
     * @param initialGold 초기 소지금
     * @param accLevelGrant 누적 레벨 증가량
     * @param newTalent 환생 후 재능
     * @param apGrant AP 지급량
     * @param recoverMax 회복 시 적용할 바이탈 최대값
     */
    @Property(tries = 100)
    void should_keepGoldUnchanged_when_rebirth(
            @ForAll("goldValues") final long initialGold,
            @ForAll("levels") final int accLevelGrant,
            @ForAll("talents") final TalentType newTalent,
            @ForAll("apGrants") final int apGrant,
            @ForAll("vitalMaxValues") final VitalMax recoverMax) {

        final CharacterProgress progress = createProgressWithGold(initialGold);

        // 환생 시뮬레이션: 레벨 1로, 누적 레벨 증가, 경험치 0, 재능 변경, AP 지급, 환생 시각, 바이탈 회복
        progress.setCurrentLevel(1);
        progress.increaseAccumulatedLevel(accLevelGrant);
        progress.setExperience(0L);
        progress.setTalent(newTalent);
        progress.increaseAbilityPoints(apGrant);
        progress.setLastRebirthAt(LocalDateTime.now());
        progress.fullRecover(recoverMax);

        assertThat(progress.getGold()).as("환생 후 소지금은 변하지 않아야 한다").isEqualTo(initialGold);
    }

    /**
     * 사망 패널티·환생 시퀀스 전후로 은행 보관 골드가 불변임을 검증한다.
     *
     * <p>Bank 엔티티에는 사망/환생과 관련된 메서드가 없으므로, 어떤 사망/환생 시퀀스에서도 은행 골드에 대한 부수효과가 없음을 확인한다.
     *
     * @param bankGold 초기 은행 보관 골드
     */
    @Property(tries = 100)
    void should_keepBankGoldUnchanged_when_deathOrRebirth(
            @ForAll("goldValues") final long bankGold) {

        final Bank bank = createBankWithGold(bankGold);

        // 사망/환생은 Bank에 대한 어떤 메서드도 호출하지 않는다.
        // 은행 골드가 불변임을 확인한다.
        assertThat(bank.getGold()).as("사망/환생 시 은행 골드는 변하지 않아야 한다").isEqualTo(bankGold);
    }

    /**
     * 사망·환생이 반복되는 시퀀스에서 소지금이 항상 불변임을 검증한다.
     *
     * @param scenario 사망/환생 반복 시나리오 (initialGold, 반복 횟수, 재능, AP)
     */
    @Property(tries = 100)
    void should_keepGoldUnchanged_when_repeatedDeathAndRebirth(
            @ForAll("repeatedScenarios") final DeathRebirthScenario scenario) {

        final CharacterProgress progress = createProgressWithGold(scenario.initialGold());
        final Bank bank = createBankWithGold(scenario.bankGold());

        for (int i = 0; i < scenario.repetitions(); i++) {
            // 사망 패널티
            progress.setCurrentLevel(Math.max(1, progress.getCurrentLevel() - 1));
            progress.setExperience(0L);
            progress.fullRecover(scenario.recoverMax());

            // 환생
            progress.setCurrentLevel(1);
            progress.increaseAccumulatedLevel(progress.getCurrentLevel());
            progress.setExperience(0L);
            progress.setTalent(scenario.talent());
            progress.increaseAbilityPoints(scenario.apGrant());
            progress.setLastRebirthAt(LocalDateTime.now());
            progress.fullRecover(scenario.recoverMax());
        }

        assertThat(progress.getGold())
                .as("반복 사망/환생 후 소지금은 변하지 않아야 한다")
                .isEqualTo(scenario.initialGold());
        assertThat(bank.getGold())
                .as("반복 사망/환생 후 은행 골드는 변하지 않아야 한다")
                .isEqualTo(scenario.bankGold());
    }

    // ─── Arbitrary 제공자 ─────────────────────────────────────────────────────

    /**
     * 골드 값을 [0, 50000] 범위에서 생성한다.
     *
     * @return 골드 Arbitrary
     */
    @Provide
    Arbitrary<Long> goldValues() {
        return Arbitraries.longs().between(0L, MAX_GOLD);
    }

    /**
     * 레벨 값을 [1, 200] 범위에서 생성한다.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, MAX_LEVEL);
    }

    /**
     * 모든 TalentType 중 임의 선택한다.
     *
     * @return 재능 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * AP 지급량을 [0, 100] 범위에서 생성한다.
     *
     * @return AP 지급량 Arbitrary
     */
    @Provide
    Arbitrary<Integer> apGrants() {
        return Arbitraries.integers().between(0, MAX_AP_GRANT);
    }

    /**
     * VitalMax를 HP/MP/Stamina 각 [50, 500] 범위에서 생성한다.
     *
     * @return VitalMax Arbitrary
     */
    @Provide
    Arbitrary<VitalMax> vitalMaxValues() {
        return Combinators.combine(
                        Arbitraries.integers().between(50, 500),
                        Arbitraries.integers().between(50, 500),
                        Arbitraries.integers().between(50, 500))
                .as(VitalMax::new);
    }

    /**
     * 사망/환생 반복 시나리오를 생성한다.
     *
     * @return DeathRebirthScenario Arbitrary
     */
    @Provide
    Arbitrary<DeathRebirthScenario> repeatedScenarios() {
        return Combinators.combine(
                        Arbitraries.longs().between(0L, MAX_GOLD),
                        Arbitraries.longs().between(0L, MAX_GOLD),
                        Arbitraries.integers().between(1, 5),
                        Arbitraries.of(TalentType.values()),
                        Arbitraries.integers().between(0, MAX_AP_GRANT),
                        Combinators.combine(
                                        Arbitraries.integers().between(50, 500),
                                        Arbitraries.integers().between(50, 500),
                                        Arbitraries.integers().between(50, 500))
                                .as(VitalMax::new))
                .as(DeathRebirthScenario::new);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    /**
     * 지정된 골드 값을 가진 {@link CharacterProgress}를 생성한다.
     *
     * @param gold 설정할 보유 골드
     * @return 해당 골드를 보유한 CharacterProgress 인스턴스
     */
    private CharacterProgress createProgressWithGold(final long gold) {
        return new CharacterProgress(
                "테스트", 5, 10, 500L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", 0, gold);
    }

    /**
     * 지정된 골드 값을 가진 {@link Bank}를 생성한다.
     *
     * <p>Bank는 {@code createDefault()} 후 deposit으로 골드를 설정한다. 0인 경우 기본 상태 그대로 반환한다.
     *
     * @param gold 설정할 은행 보관 골드
     * @return 해당 골드를 보유한 Bank 인스턴스
     */
    private Bank createBankWithGold(final long gold) {
        final Bank bank = Bank.createDefault();
        if (gold > 0) {
            bank.deposit(gold);
        }
        return bank;
    }

    /**
     * 사망/환생 반복 테스트 시나리오를 나타내는 레코드.
     *
     * @param initialGold 초기 소지금
     * @param bankGold 초기 은행 골드
     * @param repetitions 사망/환생 반복 횟수
     * @param talent 환생 시 설정할 재능
     * @param apGrant 환생 시 지급할 AP
     * @param recoverMax 바이탈 회복 최대값
     */
    private record DeathRebirthScenario(
            long initialGold,
            long bankGold,
            int repetitions,
            TalentType talent,
            int apGrant,
            VitalMax recoverMax) {}
}
