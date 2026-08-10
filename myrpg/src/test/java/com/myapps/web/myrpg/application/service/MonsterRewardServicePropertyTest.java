package com.myapps.web.myrpg.application.service;

import java.util.List;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.ItemDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 드랍 골드 범위·아이템 확률 프로퍼티 테스트.
 *
 * <p>임의의 {@link GoldDrop}과 roll 값에 대해 {@code goldFor}가 항상 {@code [min, max]} 범위의
 * 값을 반환하고, 아이템 확률 경계(100%/1%)와 수량 범위를 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 8: 드랍 골드 범위·아이템 확률
 *
 * <p><b>Validates: Requirements 8.2, 8.3, 8.4</b>
 */
class MonsterRewardServicePropertyTest {

    private static final int GOLD_MAX_UPPER = 10_000;
    private static final int QUANTITY_MAX_UPPER = 99;
    private static final List<String> DEFAULT_LINES = List.of("소리", "행동1", "행동2");

    /**
     * 임의의 유효한 {@code GoldDrop(min, max)}과 roll 값에 대해,
     * {@code goldFor}는 항상 {@code [min, max]} 범위의 값을 반환한다.
     *
     * @param goldDrop 임의 생성된 유효 골드 드랍 범위
     * @param roll     임의의 비음수 정수
     */
    @Property(tries = 100)
    void should_returnGoldInRange_when_anyValidGoldDropAndRoll(
            @ForAll("validGoldDrop") final GoldDrop goldDrop,
            @ForAll("nonNegativeRoll") final int roll) {

        final MonsterRewardService service = new MonsterRewardService(new Random(0L));

        final long result = service.goldFor(goldDrop, roll);

        assertThat(result)
                .isGreaterThanOrEqualTo(goldDrop.min())
                .isLessThanOrEqualTo(goldDrop.max());
    }

    /**
     * {@code min == max}인 {@code GoldDrop}에 대해,
     * {@code goldFor}는 roll 값에 관계없이 항상 그 고정값을 반환한다.
     *
     * @param fixedValue 골드 드랍 고정값
     * @param roll       임의의 비음수 정수
     */
    @Property(tries = 100)
    void should_returnExactValue_when_goldDropMinEqualsMax(
            @ForAll("fixedGoldValue") final int fixedValue,
            @ForAll("nonNegativeRoll") final int roll) {

        final GoldDrop goldDrop = new GoldDrop(fixedValue, fixedValue);
        final MonsterRewardService service = new MonsterRewardService(new Random(0L));

        final long result = service.goldFor(goldDrop, roll);

        assertThat(result).isEqualTo(fixedValue);
    }

    /**
     * {@code chancePercent=100}인 아이템은 {@code rollDrop}에서 항상 드랍된다.
     * 내부 Random이 0~99 모든 값을 반환해도 100% 아이템은 반드시 포함된다.
     *
     * @param seed   Random 시드 값
     * @param goldDrop 임의 생성된 유효 골드 드랍 범위
     */
    @Property(tries = 100)
    void should_alwaysDrop_when_chancePercentIs100(
            @ForAll("seeds") final long seed,
            @ForAll("validGoldDrop") final GoldDrop goldDrop) {

        final String itemId = "guaranteed-item";
        final ItemDrop itemDrop = new ItemDrop(itemId, 100, 1, 5);
        final Monster monster = buildMonster(goldDrop, List.of(itemDrop));

        final MonsterRewardService service = new MonsterRewardService(new Random(seed));

        final DropResult result = service.rollDrop(monster);

        assertThat(result.items())
                .extracting(DroppedItem::itemId)
                .contains(itemId);
    }

    /**
     * {@code chancePercent=1}이고 Random이 99를 반환하면 아이템이 드랍되지 않는다.
     * (chancePercent 범위가 1~100이므로 0은 테스트하지 않음)
     *
     * @param goldDrop 임의 생성된 유효 골드 드랍 범위
     */
    @Property(tries = 100)
    void should_notDrop_when_chancePercent1AndRollIs99(
            @ForAll("validGoldDrop") final GoldDrop goldDrop) {

        final String itemId = "rare-item";
        final ItemDrop itemDrop = new ItemDrop(itemId, 1, 1, 1);
        final Monster monster = buildMonster(goldDrop, List.of(itemDrop));

        // Random이 chanceRoll에서 99를 반환하도록 제어: 첫 nextInt는 골드용, 둘째가 chance용
        // goldFor는 nextInt(Integer.MAX_VALUE), 그 다음 chanceRoll은 nextInt(100)
        // 99 >= 1 이므로 드랍하지 않아야 한다
        final Random controlledRandom = new Random() {
            private int callCount = 0;

            @Override
            public int nextInt(final int bound) {
                callCount++;
                if (callCount == 1) {
                    // 골드 roll (임의 값)
                    return 0;
                }
                // chanceRoll: 99 반환 (bound=100이므로 유효)
                return 99;
            }
        };

        final MonsterRewardService service = new MonsterRewardService(controlledRandom);

        final DropResult result = service.rollDrop(monster);

        assertThat(result.items())
                .extracting(DroppedItem::itemId)
                .doesNotContain(itemId);
    }

    /**
     * 드랍된 아이템의 수량은 항상 {@code [minQuantity, maxQuantity]} 범위에 있다.
     *
     * @param seed         Random 시드 값
     * @param quantityRange 임의 생성된 유효 수량 범위 (min, max)
     * @param goldDrop     임의 생성된 유효 골드 드랍 범위
     */
    @Property(tries = 100)
    void should_dropQuantityInRange_when_itemDrops(
            @ForAll("seeds") final long seed,
            @ForAll("validQuantityRange") final int[] quantityRange,
            @ForAll("validGoldDrop") final GoldDrop goldDrop) {

        final int minQuantity = quantityRange[0];
        final int maxQuantity = quantityRange[1];
        final String itemId = "quantity-test-item";
        final ItemDrop itemDrop = new ItemDrop(itemId, 100, minQuantity, maxQuantity);
        final Monster monster = buildMonster(goldDrop, List.of(itemDrop));

        final MonsterRewardService service = new MonsterRewardService(new Random(seed));

        final DropResult result = service.rollDrop(monster);

        assertThat(result.items()).isNotEmpty();
        for (final DroppedItem droppedItem : result.items()) {
            assertThat(droppedItem.quantity())
                    .isGreaterThanOrEqualTo(minQuantity)
                    .isLessThanOrEqualTo(maxQuantity);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arbitrary Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 유효한 {@code GoldDrop(min, max)} 범위를 생성하는 Arbitrary 제공자.
     *
     * @return 유효 골드 드랍 범위 Arbitrary
     */
    @Provide
    Arbitrary<GoldDrop> validGoldDrop() {
        return Arbitraries.integers().between(0, GOLD_MAX_UPPER)
                .flatMap(min -> Arbitraries.integers().between(min, min + GOLD_MAX_UPPER)
                        .map(max -> new GoldDrop(min, max)));
    }

    /**
     * 비음수 roll 값을 생성하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> nonNegativeRoll() {
        return Arbitraries.integers().between(0, Integer.MAX_VALUE - 1);
    }

    /**
     * min == max인 골드 고정값을 생성하는 Arbitrary 제공자.
     *
     * @return 0 이상의 골드 고정값 Arbitrary
     */
    @Provide
    Arbitrary<Integer> fixedGoldValue() {
        return Arbitraries.integers().between(0, GOLD_MAX_UPPER);
    }

    /**
     * Random 시드 값을 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 long 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs();
    }

    /**
     * 유효한 수량 범위 {@code [minQuantity, maxQuantity]}를 생성하는 Arbitrary 제공자.
     *
     * @return 유효 수량 범위 배열 Arbitrary (index 0 = min, index 1 = max)
     */
    @Provide
    Arbitrary<int[]> validQuantityRange() {
        return Arbitraries.integers().between(1, QUANTITY_MAX_UPPER)
                .flatMap(min -> Arbitraries.integers().between(min, QUANTITY_MAX_UPPER)
                        .map(max -> new int[]{min, max}));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────────────────────

    private Monster buildMonster(final GoldDrop goldDrop, final List<ItemDrop> itemDrops) {
        return new Monster(
                "test-monster",
                "테스트몬스터",
                MonsterType.NORMAL,
                1,
                25,
                4,
                1,
                10,
                15L,
                goldDrop,
                itemDrops,
                DEFAULT_LINES
        );
    }
}
