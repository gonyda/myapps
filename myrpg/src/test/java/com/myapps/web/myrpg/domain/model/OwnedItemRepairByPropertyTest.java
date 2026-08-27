package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * OwnedItem.repairBy 연산 및 max 상한 불변 프로퍼티 테스트.
 *
 * <p>임의의 현재 내구도(currentDurability >= 0.0)와 최대 내구도(max > 0.0)에 대해, {@code repairBy(1.0, max)} 호출 후
 * 내구도가 {@code min(max, currentDurability + 1.0)}과 정확히 일치하고, 소수점 오차가 누적되지 않으며, max를 초과하지 않음을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 5: 수리 도메인 repairBy 연산 및 max 상한 불변
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3</b>
 */
class OwnedItemRepairByPropertyTest {

    private static final double MAX_DURABILITY_UPPER = 100.0;
    private static final int MAX_REPAIR_SEQUENCE = 5;

    /**
     * repairBy(1.0, max) 후의 내구도가 min(max, currentDurability + 1.0)과 정확히 일치함을 검증한다.
     *
     * @param tuple (현재 내구도, 최대 내구도) 쌍
     */
    @Property(tries = 100)
    void should_matchExactFormula_when_repairByOne(
            @ForAll("durabilityAndMax") final Tuple.Tuple2<Double, Double> tuple) {

        final double currentDurability = tuple.get1();
        final double maxDurability = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(currentDurability);

        item.repairBy(1.0, maxDurability);

        final double expected = Math.min(maxDurability, currentDurability + 1.0);
        assertThat(item.getCurrentDurability()).isEqualTo(expected);
    }

    /**
     * repairBy(1.0, max) 후에도 내구도가 max를 초과하지 않음을 검증한다.
     *
     * @param tuple (현재 내구도, 최대 내구도) 쌍
     */
    @Property(tries = 100)
    void should_notExceedMax_when_repairBy(
            @ForAll("durabilityAndMax") final Tuple.Tuple2<Double, Double> tuple) {

        final double currentDurability = tuple.get1();
        final double maxDurability = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(currentDurability);

        item.repairBy(1.0, maxDurability);

        assertThat(item.getCurrentDurability()).isLessThanOrEqualTo(maxDurability);
    }

    /**
     * 반복 repairBy(1.0, max) 후에도 소수점 오차가 누적되지 않고 각 단계가 min(max, prev + 1.0)과 정확히 일치함을 검증한다.
     *
     * @param tuple (현재 내구도, 최대 내구도) 쌍
     */
    @Property(tries = 100)
    void should_notAccumulateFloatError_when_repeatedRepairBy(
            @ForAll("durabilityAndMax") final Tuple.Tuple2<Double, Double> tuple) {

        final double currentDurability = tuple.get1();
        final double maxDurability = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(currentDurability);

        double expectedDurability = currentDurability;
        for (int i = 0; i < MAX_REPAIR_SEQUENCE; i++) {
            item.repairBy(1.0, maxDurability);
            expectedDurability = Math.min(maxDurability, expectedDurability + 1.0);
            assertThat(item.getCurrentDurability()).isEqualTo(expectedDurability);
        }

        assertThat(item.getCurrentDurability()).isLessThanOrEqualTo(maxDurability);
    }

    /**
     * reduceMaxDurability 호출 시 최대 내구도가 1 이상으로 유지되며, 현재 내구도가 유효 최대 내구도를 초과하지 않음을 검증한다.
     *
     * @param catalogMax 카탈로그 최대 내구도 (1~100)
     * @param currentDurability 현재 내구도 (0~100)
     * @param reduceAmount 감소량 (1~10)
     */
    @Property(tries = 100)
    void should_maintainValidMaxAndClampCurrent_when_reduceMaxDurability(
            @ForAll("catalogMaxArbitrary") final int catalogMax,
            @ForAll("currentDurabilityArbitrary") final double currentDurability,
            @ForAll("reduceAmountArbitrary") final int reduceAmount) {

        final OwnedItem item = createEquipmentWithDurability(currentDurability);
        final int initialMax = item.effectiveMaxDurability(catalogMax);

        item.reduceMaxDurability(reduceAmount, catalogMax);

        final int expectedMax = Math.max(1, initialMax - reduceAmount);
        assertThat(item.effectiveMaxDurability(catalogMax)).isEqualTo(expectedMax);
        assertThat(item.getCurrentDurability()).isLessThanOrEqualTo((double) expectedMax);
        assertThat(item.effectiveMaxDurability(catalogMax)).isGreaterThanOrEqualTo(1);
    }

    @Provide
    Arbitrary<Integer> catalogMaxArbitrary() {
        return Arbitraries.integers().between(1, 100);
    }

    @Provide
    Arbitrary<Double> currentDurabilityArbitrary() {
        return Arbitraries.doubles().between(0.0, 100.0);
    }

    @Provide
    Arbitrary<Integer> reduceAmountArbitrary() {
        return Arbitraries.integers().between(1, 10);
    }

    /**
     * (현재 내구도, 최대 내구도) 쌍을 생성하는 Arbitrary 제공자.
     *
     * @return (currentDurability ∈ [0,100], max ∈ (0,100]) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Double, Double>> durabilityAndMax() {
        return Arbitraries.doubles()
                .between(0.0, MAX_DURABILITY_UPPER)
                .flatMap(
                        curr ->
                                Arbitraries.doubles()
                                        .between(0.1, MAX_DURABILITY_UPPER)
                                        .map(max -> Tuple.of(curr, max)));
    }

    /**
     * 지정된 내구도로 장비 인스턴스를 생성한다.
     *
     * @param durability 초기 현재 내구도
     * @return 설정된 내구도를 가진 OwnedItem
     */
    private OwnedItem createEquipmentWithDurability(final double durability) {
        return new OwnedItem("test_equipment", 1, StorageKind.INVENTORY, false, durability);
    }
}
