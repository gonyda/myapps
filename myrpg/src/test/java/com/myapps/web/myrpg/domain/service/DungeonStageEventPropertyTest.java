package com.myapps.web.myrpg.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.StageEventType;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.template.DungeonSpawn;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DungeonService 스테이지 이벤트·몬스터 선택 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 보스 스테이지 고정 전투, 몬스터 선택 풀 제한,
 * 스테이지 이벤트 분포 집합의 불변식을 검증한다.
 *
 * <p><b>Validates: Requirements 19.3, 19.4, 20.1</b>
 */
class DungeonStageEventPropertyTest {

    private static final long BOSS_ID = 999L;
    private static final int BOSS_STAGE = 5;
    private static final int NORMAL_STAGE_MIN = 1;
    private static final int NORMAL_STAGE_MAX = 4;

    private static final double BATTLE_THRESHOLD = 0.75;
    private static final double REST_THRESHOLD = 0.83;
    private static final double MERCHANT_THRESHOLD = 0.90;
    private static final double TRAP_THRESHOLD = 0.95;

    private static final Set<StageEventType> ALL_EVENT_TYPES = Set.of(
            StageEventType.BATTLE, StageEventType.REST, StageEventType.MERCHANT,
            StageEventType.TRAP, StageEventType.TREASURE);

    // --- Providers ---

    @Provide
    Arbitrary<Double> rollProvider() {
        return Arbitraries.doubles().between(0.0, true, 1.0, false);
    }

    @Provide
    Arbitrary<Integer> normalStageProvider() {
        return Arbitraries.integers().between(NORMAL_STAGE_MIN, NORMAL_STAGE_MAX);
    }

    @Provide
    Arbitrary<Integer> monsterSelectRollProvider() {
        return Arbitraries.integers().between(0, 9999);
    }

    // =====================================================================
    // Property 35: 5스테이지는 항상 보스 전투
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 35: 5스테이지는 항상 보스 전투
    /**
     * 임의의 roll 값 [0.0, 1.0)에 대해 rollStageEvent(5)는 항상
     * StageEventType.BATTLE을 반환한다. 보스 스테이지는 난수와 무관하다.
     *
     * <p><b>Validates: Requirements 19.3</b>
     */
    @Property(tries = 100)
    void bossStageAlwaysReturnsBattle(
            @ForAll("rollProvider") final double roll) {

        final FixedRandomSource random = new FixedRandomSource(roll);
        final DungeonService service = new DungeonService(random);

        final StageEventType result = service.rollStageEvent(BOSS_STAGE);

        assertEquals(StageEventType.BATTLE, result,
                "5스테이지는 roll=" + roll + "과 무관하게 항상 BATTLE이어야 한다");
    }

    // =====================================================================
    // Property 36: 몬스터 선택은 스테이지 풀 안에서
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 36: 몬스터 선택은 스테이지 풀 안에서
    /**
     * 임의의 스테이지(1~4)와 난수 roll에 대해 pickMonster가 반환하는 몬스터 ID는
     * 항상 해당 스테이지의 eligible 풀(minFloor <= stage <= maxFloor)에 속한다.
     * 스테이지 5일 때는 항상 bossId를 반환한다.
     *
     * <p><b>Validates: Requirements 19.4</b>
     */
    @Property(tries = 100)
    void pickMonsterReturnsEligibleMonsterForNormalStage(
            @ForAll("normalStageProvider") final int stage,
            @ForAll("monsterSelectRollProvider") final int rollInt) {

        final List<DungeonSpawn> monsters = List.of(
                new DungeonSpawn(101L, 1, 2, 10),
                new DungeonSpawn(102L, 1, 4, 5),
                new DungeonSpawn(103L, 3, 4, 8),
                new DungeonSpawn(104L, 2, 3, 3)
        );

        final DungeonTemplate dungeon = createDungeonTemplate(monsters);

        final Set<Long> eligible = monsters.stream()
                .filter(spawn -> spawn.minFloor() <= stage && stage <= spawn.maxFloor())
                .map(DungeonSpawn::monsterId)
                .collect(java.util.stream.Collectors.toSet());

        final int totalWeight = monsters.stream()
                .filter(spawn -> spawn.minFloor() <= stage && stage <= spawn.maxFloor())
                .mapToInt(DungeonSpawn::spawnWeight)
                .sum();

        final int boundedRoll = rollInt % totalWeight;
        final FixedRandomSource random = new FixedRandomSource(new double[0], new int[]{boundedRoll});
        final DungeonService service = new DungeonService(random);

        final long result = service.pickMonster(dungeon, stage);

        assertTrue(eligible.contains(result),
                "stage=" + stage + "에서 선택된 몬스터 " + result
                        + "는 eligible 풀 " + eligible + "에 속해야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 36: 몬스터 선택은 스테이지 풀 안에서
    /**
     * 스테이지 5에서 pickMonster는 항상 던전 템플릿의 bossId를 반환한다.
     *
     * <p><b>Validates: Requirements 19.4</b>
     */
    @Property(tries = 100)
    void pickMonsterReturnsBossIdForStage5(
            @ForAll("rollProvider") final double roll) {

        final List<DungeonSpawn> monsters = List.of(
                new DungeonSpawn(101L, 1, 4, 10),
                new DungeonSpawn(102L, 2, 4, 5)
        );

        final DungeonTemplate dungeon = createDungeonTemplate(monsters);

        final FixedRandomSource random = new FixedRandomSource(roll);
        final DungeonService service = new DungeonService(random);

        final long result = service.pickMonster(dungeon, BOSS_STAGE);

        assertEquals(BOSS_ID, result,
                "스테이지 5에서는 roll=" + roll + "과 무관하게 bossId=" + BOSS_ID + "를 반환해야 한다");
    }

    // =====================================================================
    // Property 37: 스테이지 이벤트 분포 집합
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 37: 스테이지 이벤트 분포 집합
    /**
     * 임의의 스테이지(1~4)와 roll ∈ [0.0, 1.0)에 대해 rollStageEvent는
     * {BATTLE, REST, MERCHANT, TRAP, TREASURE} 집합에서만 값을 반환하며,
     * 누적 분포 구간에 맞는 정확한 이벤트를 반환한다.
     *
     * <p>분포 임계값:
     * <ul>
     *   <li>BATTLE: roll < 0.75</li>
     *   <li>REST: 0.75 <= roll < 0.83</li>
     *   <li>MERCHANT: 0.83 <= roll < 0.90</li>
     *   <li>TRAP: 0.90 <= roll < 0.95</li>
     *   <li>TREASURE: roll >= 0.95</li>
     * </ul>
     *
     * <p><b>Validates: Requirements 20.1</b>
     */
    @Property(tries = 100)
    void stageEventMatchesDistributionThresholds(
            @ForAll("normalStageProvider") final int stage,
            @ForAll("rollProvider") final double roll) {

        final FixedRandomSource random = new FixedRandomSource(roll);
        final DungeonService service = new DungeonService(random);

        final StageEventType result = service.rollStageEvent(stage);

        assertTrue(ALL_EVENT_TYPES.contains(result),
                "결과 " + result + "는 유효한 이벤트 집합에 속해야 한다");

        final StageEventType expected = expectedEventForRoll(roll);
        assertEquals(expected, result,
                "stage=" + stage + ", roll=" + roll + " → 기대=" + expected
                        + " 실제=" + result);
    }

    // --- Helper methods ---

    /**
     * roll 값에 기반하여 기대되는 스테이지 이벤트를 계산한다.
     *
     * @param roll 난수 값 [0.0, 1.0)
     * @return 기대되는 스테이지 이벤트 종류
     */
    private StageEventType expectedEventForRoll(final double roll) {
        if (roll < BATTLE_THRESHOLD) {
            return StageEventType.BATTLE;
        }
        if (roll < REST_THRESHOLD) {
            return StageEventType.REST;
        }
        if (roll < MERCHANT_THRESHOLD) {
            return StageEventType.MERCHANT;
        }
        if (roll < TRAP_THRESHOLD) {
            return StageEventType.TRAP;
        }
        return StageEventType.TREASURE;
    }

    /**
     * 테스트용 DungeonTemplate을 생성한다.
     *
     * @param monsters 몬스터 스폰 목록
     * @return 던전 템플릿
     */
    private DungeonTemplate createDungeonTemplate(final List<DungeonSpawn> monsters) {
        return new DungeonTemplate(
                1L,
                "테스트 던전",
                1,
                5,
                1,
                BOSS_ID,
                1,
                List.of(),
                List.of(),
                Map.of(),
                100,
                monsters
        );
    }
}
