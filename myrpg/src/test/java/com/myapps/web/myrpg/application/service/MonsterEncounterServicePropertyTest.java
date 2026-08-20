package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 선공 판정 경계·선택 프로퍼티 테스트.
 *
 * <p>{@code triggers(roll)} 순수 함수의 경계값 판정과 {@code rollPreemptiveStrike}의 빈 목록·발동 시 선택·결정성을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 9: 선공 판정 경계·선택
 *
 * <p><b>Validates: Requirements 9.1, 9.2, 9.3</b>
 */
class MonsterEncounterServicePropertyTest {

    private static final int PREEMPTIVE_THRESHOLD = 5;
    private static final int PERCENT_BOUND = 100;
    private static final int MAX_MONSTER_COUNT = 5;
    private static final long FIXED_SEED = 42L;

    // ──────────────────────────────────────────────────────────────────────
    // Property 9-1: triggers 경계 판정
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 0~99 범위의 모든 roll에 대해, {@code triggers}는 {@code roll < 5}일 때만 true를 반환한다.
     *
     * @param roll 0 이상 99 이하의 임의 정수
     */
    @Property(tries = 100)
    void should_returnTrue_when_rollBelowThreshold(@ForAll("rollValues") final int roll) {
        final MonsterEncounterService service = new MonsterEncounterService(new Random(FIXED_SEED));

        final boolean result = service.triggers(roll);

        assertThat(result).isEqualTo(roll < PREEMPTIVE_THRESHOLD);
    }

    /** 경계값 4는 발동(true), 5는 미발동(false)을 정확히 반환함을 검증한다. */
    @Property(tries = 100)
    void should_triggerAtFour_and_notTriggerAtFive() {
        final MonsterEncounterService service = new MonsterEncounterService(new Random(FIXED_SEED));

        assertThat(service.triggers(4)).isTrue();
        assertThat(service.triggers(5)).isFalse();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 9-2: 빈 목록 → 항상 empty
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 빈 목록으로 {@code rollPreemptiveStrike}를 호출하면 항상 빈 Optional을 반환한다.
     *
     * @param seed 임의의 시드 (어떤 시드든 빈 목록에서는 empty)
     */
    @Property(tries = 100)
    void should_returnEmpty_when_monsterListIsEmpty(@ForAll("seeds") final long seed) {
        final MonsterEncounterService service = new MonsterEncounterService(new Random(seed));

        final Optional<Monster> result = service.rollPreemptiveStrike(List.of());

        assertThat(result).isEmpty();
    }

    /**
     * null로 {@code rollPreemptiveStrike}를 호출하면 항상 빈 Optional을 반환한다.
     *
     * @param seed 임의의 시드
     */
    @Property(tries = 100)
    void should_returnEmpty_when_monsterListIsNull(@ForAll("seeds") final long seed) {
        final MonsterEncounterService service = new MonsterEncounterService(new Random(seed));

        final Optional<Monster> result = service.rollPreemptiveStrike(null);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 9-3: 발동 시 반환 몬스터 ∈ 입력 목록
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 선공 발동 시 반환된 몬스터가 항상 입력 목록에 포함됨을 검증한다. 확실히 발동하는 시드(roll < 5)를 사용한다.
     *
     * @param monsters 임의 생성된 몬스터 목록 (1~5개)
     */
    @Property(tries = 100)
    void should_returnMonsterFromInput_when_triggered(
            @ForAll("monsterLists") final List<Monster> monsters) {

        // roll이 0~4 범위에 들도록 시드를 찾아 사용 (확실히 발동하는 Random)
        final Random triggeringRandom = buildTriggeringRandom(monsters.size());
        final MonsterEncounterService service = new MonsterEncounterService(triggeringRandom);

        final Optional<Monster> result = service.rollPreemptiveStrike(monsters);

        assertThat(result).isPresent();
        assertThat(monsters).contains(result.get());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 9-4: 고정 시드 결정성
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 동일 시드의 Random으로 동일 입력을 두 번 호출하면 동일 결과를 반환함을 검증한다.
     *
     * @param monsters 임의 생성된 몬스터 목록 (1~5개)
     * @param seed 임의의 시드값
     */
    @Property(tries = 100)
    void should_beDeterministic_when_sameSeedUsed(
            @ForAll("monsterLists") final List<Monster> monsters,
            @ForAll("seeds") final long seed) {

        final MonsterEncounterService service1 = new MonsterEncounterService(new Random(seed));
        final MonsterEncounterService service2 = new MonsterEncounterService(new Random(seed));

        final Optional<Monster> result1 = service1.rollPreemptiveStrike(monsters);
        final Optional<Monster> result2 = service2.rollPreemptiveStrike(monsters);

        assertThat(result1).isEqualTo(result2);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arbitrary Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 0~99 범위의 정수를 생성하는 Arbitrary 제공자.
     *
     * @return 0~99 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> rollValues() {
        return Arbitraries.integers().between(0, PERCENT_BOUND - 1);
    }

    /**
     * 임의의 시드를 생성하는 Arbitrary 제공자.
     *
     * @return long 타입 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs();
    }

    /**
     * 유효한 Monster 목록(1~5개)을 생성하는 Arbitrary 제공자.
     *
     * @return Monster 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<Monster>> monsterLists() {
        return monsterArbitrary().list().ofMinSize(1).ofMaxSize(MAX_MONSTER_COUNT);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Monster Arbitrary
    // ──────────────────────────────────────────────────────────────────────

    private Arbitrary<Monster> monsterArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        final Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(8);
        final Arbitrary<MonsterType> types = Arbitraries.of(MonsterType.values());
        final Arbitrary<Integer> levels = Arbitraries.integers().between(1, 50);
        final Arbitrary<Integer> maxHps = Arbitraries.integers().between(1, 999);
        final Arbitrary<Integer> attackPowers = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> defenses = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> criticals = Arbitraries.integers().between(0, 500);
        final Arbitrary<Long> experiences = Arbitraries.longs().between(1L, 10000L);
        final Arbitrary<GoldDrop> goldDrops =
                Arbitraries.integers()
                        .between(0, 100)
                        .flatMap(
                                min ->
                                        Arbitraries.integers()
                                                .between(min, min + 100)
                                                .map(max -> new GoldDrop(min, max)));
        final Arbitrary<List<String>> lines =
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(15).list().ofSize(3);

        return Combinators.combine(
                        ids, names, types, levels, maxHps, attackPowers, defenses, criticals)
                .as(
                        (id, name, type, level, maxHp, atk, def, crit) ->
                                new MonsterPartial(id, name, type, level, maxHp, atk, def, crit))
                .flatMap(
                        partial ->
                                Combinators.combine(experiences, goldDrops, lines)
                                        .as(
                                                (exp, gd, ln) ->
                                                        new Monster(
                                                                partial.id(),
                                                                partial.name(),
                                                                partial.type(),
                                                                partial.level(),
                                                                partial.maxHp(),
                                                                partial.attackPower(),
                                                                partial.defense(),
                                                                partial.critical(),
                                                                exp,
                                                                gd,
                                                                List.of(),
                                                                ln)));
    }

    /** Monster 생성 시 인자 분할을 위한 중간 레코드. */
    private record MonsterPartial(
            String id,
            String name,
            MonsterType type,
            int level,
            int maxHp,
            int attackPower,
            int defense,
            int critical) {}

    // ──────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 확실히 선공 발동하는 Random을 생성한다. 내부에서 nextInt(100)이 0~4 범위를 반환하도록 시드를 탐색한다.
     *
     * @param monsterCount 몬스터 목록 크기 (선택 인덱스 결정에 사용)
     * @return 발동이 보장되는 Random
     */
    private Random buildTriggeringRandom(final int monsterCount) {
        for (long seed = 0; seed < 10000; seed++) {
            final Random candidate = new Random(seed);
            final int roll = candidate.nextInt(PERCENT_BOUND);
            if (roll < PREEMPTIVE_THRESHOLD) {
                return new Random(seed);
            }
        }
        // 사실상 도달할 수 없음 — 10000개 시드 중 ~500개가 발동
        return new Random(0);
    }
}
