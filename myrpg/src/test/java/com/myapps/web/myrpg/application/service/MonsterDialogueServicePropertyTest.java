package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 몬스터 조우 대사 선택 프로퍼티 테스트.
 *
 * <p>{@link MonsterDialogueService#selectLine(Monster)}이 항상 해당 몬스터의 {@code lines}에 포함된 값을 반환하고, 고정
 * 시드 {@code Random}에서 결정적이며, 폴백 문구를 사용하지 않음을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 6: 조우 대사 선택
 *
 * <p><b>Validates: Requirements 6.2, 6.3, 6.5</b>
 */
class MonsterDialogueServicePropertyTest {

    private static final int LINE_MIN_LENGTH = 1;
    private static final int LINE_MAX_LENGTH = 30;
    private static final int LINES_COUNT = 3;
    private static final long FIXED_SEED = 42L;
    private static final int DETERMINISM_ITERATIONS = 10;

    /**
     * 임의의 몬스터에 대해 {@code selectLine}의 반환값은 항상 해당 몬스터의 {@code lines} 목록에 포함된다.
     *
     * @param monster 임의 생성된 몬스터 (lines 3개 보유)
     */
    @Property(tries = 100)
    void should_returnLineFromMonsterLines_when_selectLineCalled(
            @ForAll("validMonsters") final Monster monster) {
        // Given
        final MonsterDialogueService service = new MonsterDialogueService(new Random());

        // When
        final String result = service.selectLine(monster);

        // Then: 반환값은 반드시 lines에 포함
        assertThat(monster.lines()).contains(result);
    }

    /**
     * 동일 시드의 {@code Random}으로 생성한 두 서비스 인스턴스가 동일한 몬스터에 대해 동일한 대사 선택 시퀀스를 반환함을 검증한다(결정적).
     *
     * @param monster 임의 생성된 몬스터 (lines 3개 보유)
     */
    @Property(tries = 100)
    void should_produceDeterministicResults_when_sameSeedUsed(
            @ForAll("validMonsters") final Monster monster) {
        // Given: 동일 시드로 생성된 두 서비스
        final MonsterDialogueService service1 = new MonsterDialogueService(new Random(FIXED_SEED));
        final MonsterDialogueService service2 = new MonsterDialogueService(new Random(FIXED_SEED));

        // When & Then: 동일 몬스터에 대해 반복 호출 시 동일 시퀀스
        for (int i = 0; i < DETERMINISM_ITERATIONS; i++) {
            final String result1 = service1.selectLine(monster);
            final String result2 = service2.selectLine(monster);
            assertThat(result1).as("호출 %d번째에서 동일 시드는 동일 결과를 반환해야 합니다", i).isEqualTo(result2);
        }
    }

    /**
     * {@code selectLine}의 반환값은 폴백 문구가 아니라 반드시 몬스터의 원래 {@code lines} 3개 중 하나와 정확히 일치한다.
     *
     * <p>폴백 문구가 존재하지 않으므로, 반환값이 lines의 정확한 원소임을 재확인한다.
     *
     * @param monster 임의 생성된 몬스터 (lines 3개 보유)
     */
    @Property(tries = 100)
    void should_neverUseFallbackText_when_linesAlwaysHaveThreeEntries(
            @ForAll("validMonsters") final Monster monster) {
        // Given
        final MonsterDialogueService service = new MonsterDialogueService(new Random());
        final List<String> originalLines = monster.lines();

        // When: 여러 번 호출
        for (int i = 0; i < DETERMINISM_ITERATIONS; i++) {
            final String result = service.selectLine(monster);

            // Then: 반환값은 원래 lines 중 하나와 정확히 동일(폴백 아님)
            assertThat(result)
                    .isIn(originalLines.get(0), originalLines.get(1), originalLines.get(2));
        }
    }

    /**
     * 유효한 {@link Monster}를 생성하는 Arbitrary 제공자.
     *
     * <p>각 몬스터는 정확히 3개의 비어있지 않은 고유 대사를 보유한다. jqwik {@code Combinators.combine}은 최대 8인자까지 지원하므로 두
     * 단계로 나누어 결합한다.
     *
     * @return 임의의 유효 Monster Arbitrary
     */
    @Provide
    Arbitrary<Monster> validMonsters() {
        final Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        final Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(8);
        final Arbitrary<MonsterType> types = Arbitraries.of(MonsterType.values());
        final Arbitrary<Integer> levels = Arbitraries.integers().between(1, 50);
        final Arbitrary<Integer> maxHps = Arbitraries.integers().between(10, 500);
        final Arbitrary<Integer> attackPowers = Arbitraries.integers().between(1, 100);
        final Arbitrary<Integer> defenses = Arbitraries.integers().between(0, 50);
        final Arbitrary<Integer> criticals = Arbitraries.integers().between(0, 200);

        final Arbitrary<Long> experiences = Arbitraries.longs().between(1L, 1000L);
        final Arbitrary<GoldDrop> goldDrops =
                Arbitraries.integers()
                        .between(0, 50)
                        .flatMap(
                                min ->
                                        Arbitraries.integers()
                                                .between(min, min + 50)
                                                .map(max -> new GoldDrop(min, max)));
        final Arbitrary<List<String>> linesList = uniqueThreeLines();

        return Combinators.combine(
                        ids, names, types, levels, maxHps, attackPowers, defenses, criticals)
                .flatAs(
                        (id, name, type, level, maxHp, attackPower, defense, critical) ->
                                Combinators.combine(experiences, goldDrops, linesList)
                                        .as(
                                                (experience, goldDrop, lines) ->
                                                        new Monster(
                                                                id,
                                                                name,
                                                                type,
                                                                level,
                                                                maxHp,
                                                                attackPower,
                                                                defense,
                                                                critical,
                                                                experience,
                                                                goldDrop,
                                                                List.of(),
                                                                lines)));
    }

    private Arbitrary<List<String>> uniqueThreeLines() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(LINE_MIN_LENGTH)
                .ofMaxLength(LINE_MAX_LENGTH)
                .list()
                .ofSize(LINES_COUNT)
                .filter(lines -> lines.stream().distinct().count() == LINES_COUNT);
    }
}
