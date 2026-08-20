package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * 내구도 초기화·감소·수리 프로퍼티 테스트.
 *
 * <p>임의의 장비 인스턴스에 대해, 지급 시 {@code currentDurability == maxDurability}이고, {@code
 * reduceDurability(d)}는 0 미만으로 내려가지 않으며, {@code repairToMax(max)}는 {@code currentDurability ==
 * max}로 복구함을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 14: 내구도 초기화·감소·수리
 *
 * <p><b>Validates: Requirements 17.2, 17.3, 17.4</b>
 */
class DurabilityPropertyTest {

    private static final double MAX_DURABILITY_UPPER = 100.0;
    private static final double REDUCE_UPPER = 200.0;
    private static final int MAX_REDUCE_SEQUENCE = 10;

    /**
     * 초기화: OwnedItem 생성 시 currentDurability가 생성자에 전달한 값과 일치하는지 검증한다.
     *
     * @param maxDurability 최대 내구도(생성 시 초기값으로 설정)
     */
    @Property(tries = 100)
    void should_initializeCurrentDurability_when_created(
            @ForAll("positiveDurability") final double maxDurability) {

        final OwnedItem item = createEquipmentWithDurability(maxDurability);

        assertThat(item.getCurrentDurability()).isEqualTo(maxDurability);
    }

    /**
     * 감소: reduceDurability(d) 후 currentDurability가 항상 0 이상임을 검증한다.
     *
     * @param tuple (maxDurability, reduceAmount) 쌍
     */
    @Property(tries = 100)
    void should_neverGoBelowZero_when_reduceDurability(
            @ForAll("durabilityAndReduce") final Tuple.Tuple2<Double, Double> tuple) {

        final double maxDurability = tuple.get1();
        final double reduceAmount = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(maxDurability);

        item.reduceDurability(reduceAmount);

        assertThat(item.getCurrentDurability()).isGreaterThanOrEqualTo(0.0);
    }

    /**
     * 감소: reduceDurability(d) 결과가 Math.max(0, prev - d)와 일치하는지 검증한다.
     *
     * @param tuple (maxDurability, reduceAmount) 쌍
     */
    @Property(tries = 100)
    void should_reduceExactly_when_reduceDurability(
            @ForAll("durabilityAndReduce") final Tuple.Tuple2<Double, Double> tuple) {

        final double maxDurability = tuple.get1();
        final double reduceAmount = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(maxDurability);

        item.reduceDurability(reduceAmount);

        final double expected = Math.max(0.0, maxDurability - reduceAmount);
        assertThat(item.getCurrentDurability()).isEqualTo(expected);
    }

    /**
     * 수리: repairToMax(max) 후 currentDurability가 max와 일치하는지 검증한다.
     *
     * @param tuple (초기내구도, 감소량, 수리최대값) 튜플
     */
    @Property(tries = 100)
    void should_restoreToMax_when_repairToMax(
            @ForAll("durabilityReduceAndRepair") final Tuple.Tuple3<Double, Double, Double> tuple) {

        final double initialDurability = tuple.get1();
        final double reduceAmount = tuple.get2();
        final double repairMax = tuple.get3();

        final OwnedItem item = createEquipmentWithDurability(initialDurability);
        item.reduceDurability(reduceAmount);

        item.repairToMax(repairMax);

        assertThat(item.getCurrentDurability()).isEqualTo(repairMax);
    }

    /**
     * 반복 감소 후 수리: 여러 번 reduce로 바닥(0) 도달 후 repairToMax로 복구되는지 검증한다.
     *
     * @param tuple (maxDurability, 감소량 리스트) 쌍
     */
    @Property(tries = 100)
    void should_restoreAfterRepeatedReduce_when_repairToMax(
            @ForAll("durabilityAndReduceSequence") final Tuple.Tuple2<Double, List<Double>> tuple) {

        final double maxDurability = tuple.get1();
        final List<Double> reduceSequence = tuple.get2();

        final OwnedItem item = createEquipmentWithDurability(maxDurability);

        double expectedDurability = maxDurability;
        for (final double reduceAmount : reduceSequence) {
            item.reduceDurability(reduceAmount);
            expectedDurability = Math.max(0.0, expectedDurability - reduceAmount);
            assertThat(item.getCurrentDurability()).isGreaterThanOrEqualTo(0.0);
        }

        assertThat(item.getCurrentDurability()).isEqualTo(expectedDurability);

        item.repairToMax(maxDurability);

        assertThat(item.getCurrentDurability()).isEqualTo(maxDurability);
    }

    /**
     * 양수 내구도 값을 생성하는 Arbitrary 제공자.
     *
     * @return (0, 100] 범위의 double Arbitrary
     */
    @Provide
    Arbitrary<Double> positiveDurability() {
        return Arbitraries.doubles().between(0.1, MAX_DURABILITY_UPPER);
    }

    /**
     * (maxDurability, reduceAmount) 쌍을 생성하는 Arbitrary 제공자.
     *
     * @return (maxDurability ∈ (0,100], reduceAmount ∈ [0,200]) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Double, Double>> durabilityAndReduce() {
        return Arbitraries.doubles()
                .between(0.1, MAX_DURABILITY_UPPER)
                .flatMap(
                        dur ->
                                Arbitraries.doubles()
                                        .between(0.0, REDUCE_UPPER)
                                        .map(reduce -> Tuple.of(dur, reduce)));
    }

    /**
     * (초기내구도, 감소량, 수리최대값) 튜플을 생성하는 Arbitrary 제공자.
     *
     * @return (initialDurability, reduceAmount, repairMax) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Double, Double, Double>> durabilityReduceAndRepair() {
        return Arbitraries.doubles()
                .between(0.1, MAX_DURABILITY_UPPER)
                .flatMap(
                        dur ->
                                Arbitraries.doubles()
                                        .between(0.0, REDUCE_UPPER)
                                        .flatMap(
                                                reduce ->
                                                        Arbitraries.doubles()
                                                                .between(0.1, MAX_DURABILITY_UPPER)
                                                                .map(
                                                                        repairMax ->
                                                                                Tuple.of(
                                                                                        dur, reduce,
                                                                                        repairMax))));
    }

    /**
     * (maxDurability, 감소량 리스트) 쌍을 생성하는 Arbitrary 제공자. 감소량 합이 maxDurability를 초과하여 바닥(0)에 도달하도록 구성한다.
     *
     * @return (maxDurability, reduceSequence) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Double, List<Double>>> durabilityAndReduceSequence() {
        return Arbitraries.doubles()
                .between(0.1, MAX_DURABILITY_UPPER)
                .flatMap(
                        dur ->
                                Arbitraries.doubles()
                                        .between(0.1, REDUCE_UPPER)
                                        .list()
                                        .ofMinSize(1)
                                        .ofMaxSize(MAX_REDUCE_SEQUENCE)
                                        .map(reductions -> Tuple.of(dur, reductions)));
    }

    /**
     * 지정된 내구도로 장비 인스턴스를 생성한다.
     *
     * @param durability 초기 현재 내구도 (= 최대 내구도로 간주)
     * @return 해당 내구도를 가진 OwnedItem 인스턴스
     */
    private OwnedItem createEquipmentWithDurability(final double durability) {
        return new OwnedItem("test_equipment", 1, StorageKind.INVENTORY, false, durability);
    }
}
