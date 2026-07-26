package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.Player;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Feature: myrpg-gen1-mvp, Property 7: 경험치 페널티 불변식
// Feature: myrpg-gen1-mvp, Property 8: 마을 복귀 시 완전 회복과 아이템 보존
/**
 * CharacterService.applyExpPenalty 및 restoreToTown 메서드의 속성 기반 테스트.
 *
 * <p>경험치 페널티 불변식과 마을 복귀 시 HP/MP 완전 회복 및
 * 기타 상태 보존을 검증한다.
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.4, 4.5, 4.6, 4.7</b>
 */
class CharacterServicePenaltyPropertyTest {

    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 50;
    private static final int BASE_HP = 100;
    private static final int BASE_MP = 50;
    private static final int BASE_ATTACK = 10;
    private static final int BASE_DEFENSE = 5;
    private static final int BASE_SPEED = 5;
    private static final int BASE_CRITICAL = 0;
    private static final int HP_PER_LEVEL = 20;
    private static final int MP_PER_LEVEL = 10;
    private static final int ATTACK_PER_LEVEL = 3;
    private static final int DEFENSE_PER_LEVEL = 2;
    private static final int SPEED_PER_LEVEL = 1;
    private static final int CRITICAL_PER_LEVEL = 1;
    private static final int GOLD_MAX = 100_000;

    private final CharacterService characterService = new CharacterService();

    /**
     * 유효한 플레이어 상태를 생성하는 Provider.
     *
     * <p>레벨에 따라 일관된 스탯을 가진 플레이어를 생성한다.
     * 현재 경험치는 0 이상 현재 레벨 필요 경험치 미만이다.
     *
     * @return Player Arbitrary
     */
    @Provide
    Arbitrary<Player> validPlayers() {
        return Arbitraries.integers().between(LEVEL_MIN, LEVEL_MAX).flatMap(level -> {
            final int maxHp = BASE_HP + (level - 1) * HP_PER_LEVEL;
            final int maxMp = BASE_MP + (level - 1) * MP_PER_LEVEL;
            final int attack = BASE_ATTACK + (level - 1) * ATTACK_PER_LEVEL;
            final int defense = BASE_DEFENSE + (level - 1) * DEFENSE_PER_LEVEL;
            final int speed = BASE_SPEED + (level - 1) * SPEED_PER_LEVEL;
            final int critical = BASE_CRITICAL + (level - 1) * CRITICAL_PER_LEVEL;
            final int requiredExp = characterService.requiredExp(level);
            final int maxExp = Math.max(0, requiredExp - 1);

            return Arbitraries.integers().between(0, maxExp).map(exp ->
                    new Player("TestHero", level, exp, maxHp, maxHp, maxMp, maxMp,
                            attack, defense, speed, critical, 0)
            );
        });
    }

    /**
     * 페널티 비율(0 이상 1 이하)을 생성하는 Provider.
     *
     * @return 비율 Arbitrary (0.0 ~ 1.0)
     */
    @Provide
    Arbitrary<Double> penaltyRatios() {
        return Arbitraries.doubles().between(0.0, 1.0);
    }

    /**
     * 마을 복귀 테스트용 플레이어를 생성하는 Provider.
     *
     * <p>HP와 MP가 최대치 미만인 상태(전투 후 상태)를 시뮬레이션한다.
     * 골드를 포함한 모든 스탯이 랜덤 값을 갖는다.
     *
     * @return Player Arbitrary
     */
    @Provide
    Arbitrary<Player> damagedPlayers() {
        return Arbitraries.integers().between(LEVEL_MIN, LEVEL_MAX).flatMap(level -> {
            final int maxHp = BASE_HP + (level - 1) * HP_PER_LEVEL;
            final int maxMp = BASE_MP + (level - 1) * MP_PER_LEVEL;
            final int attack = BASE_ATTACK + (level - 1) * ATTACK_PER_LEVEL;
            final int defense = BASE_DEFENSE + (level - 1) * DEFENSE_PER_LEVEL;
            final int speed = BASE_SPEED + (level - 1) * SPEED_PER_LEVEL;
            final int critical = BASE_CRITICAL + (level - 1) * CRITICAL_PER_LEVEL;
            final int requiredExp = characterService.requiredExp(level);
            final int maxExp = Math.max(0, requiredExp - 1);

            return Arbitraries.integers().between(1, maxHp).flatMap(hp ->
                    Arbitraries.integers().between(0, maxMp).flatMap(mp ->
                            Arbitraries.integers().between(0, maxExp).flatMap(exp ->
                                    Arbitraries.integers().between(0, GOLD_MAX).map(gold ->
                                            new Player("TestHero", level, exp, hp, maxHp,
                                                    mp, maxMp, attack, defense, speed,
                                                    critical, gold)
                                    )
                            )
                    )
            );
        });
    }

    // Feature: myrpg-gen1-mvp, Property 7: 경험치 페널티 불변식
    /**
     * applyExpPenalty는 현재 경험치를 정확히 floor(currentExp × ratio)만큼 감소시킨다.
     *
     * <p>결과 경험치는 0 이상 원래 경험치 이하이며, 레벨은 변경되지 않는다.
     * 원래 경험치가 0이면 감소량은 0이다.
     *
     * <p><b>Validates: Requirements 4.1, 4.2, 4.4, 4.5</b>
     *
     * @param player 테스트 대상 플레이어
     * @param ratio  페널티 비율 (0.0 ~ 1.0)
     */
    @Property(tries = 100)
    void expPenaltyInvariant(
            @ForAll("validPlayers") final Player player,
            @ForAll("penaltyRatios") final Double ratio) {

        final int originalExp = player.getExp();
        final int originalLevel = player.getLevel();
        final int expectedDeduction = (int) Math.floor(originalExp * ratio);

        final int actualDeduction = characterService.applyExpPenalty(player, ratio);

        final int resultExp = player.getExp();

        assertEquals(originalLevel, player.getLevel(),
                "페널티 적용 후 레벨은 변경되지 않아야 한다");
        assertTrue(resultExp >= 0,
                "결과 경험치는 0 이상이어야 한다: resultExp=" + resultExp);
        assertTrue(resultExp <= originalExp,
                "결과 경험치는 원래 경험치 이하여야 한다: resultExp="
                        + resultExp + ", originalExp=" + originalExp);
        assertEquals(expectedDeduction, actualDeduction,
                "감소량은 floor(currentExp × ratio)여야 한다: expected="
                        + expectedDeduction + ", actual=" + actualDeduction);
        assertEquals(originalExp - expectedDeduction, resultExp,
                "결과 경험치 = 원래 경험치 - 감소량이어야 한다");

        if (originalExp == 0) {
            assertEquals(0, actualDeduction,
                    "원래 경험치가 0이면 감소량도 0이어야 한다");
            assertEquals(0, resultExp,
                    "원래 경험치가 0이면 결과 경험치도 0이어야 한다");
        }
    }

    // Feature: myrpg-gen1-mvp, Property 8: 마을 복귀 시 완전 회복과 아이템 보존
    /**
     * restoreToTown 호출 후 HP = maxHP, MP = maxMP이며
     * 그 외 모든 플레이어 상태(레벨, 경험치, 공격력, 방어력, 속도, 치명타, 골드)는
     * 변경되지 않는다.
     *
     * <p><b>Validates: Requirements 4.6, 4.7</b>
     *
     * @param player 테스트 대상 플레이어 (HP/MP가 최대치 미만일 수 있음)
     */
    @Property(tries = 100)
    void townRestoreFullRecoveryAndStatePreservation(
            @ForAll("damagedPlayers") final Player player) {

        final int originalLevel = player.getLevel();
        final int originalExp = player.getExp();
        final int originalMaxHp = player.getMaxHp();
        final int originalMaxMp = player.getMaxMp();
        final int originalAttack = player.getAttack();
        final int originalDefense = player.getDefense();
        final int originalSpeed = player.getSpeed();
        final int originalCritical = player.getCritical();
        final int originalGold = player.getGold();

        characterService.restoreToTown(player);

        assertEquals(originalMaxHp, player.getHp(),
                "마을 복귀 후 HP는 최대 HP와 같아야 한다");
        assertEquals(originalMaxMp, player.getMp(),
                "마을 복귀 후 MP는 최대 MP와 같아야 한다");
        assertEquals(originalLevel, player.getLevel(),
                "마을 복귀 후 레벨은 변경되지 않아야 한다");
        assertEquals(originalExp, player.getExp(),
                "마을 복귀 후 경험치는 변경되지 않아야 한다");
        assertEquals(originalMaxHp, player.getMaxHp(),
                "마을 복귀 후 최대 HP는 변경되지 않아야 한다");
        assertEquals(originalMaxMp, player.getMaxMp(),
                "마을 복귀 후 최대 MP는 변경되지 않아야 한다");
        assertEquals(originalAttack, player.getAttack(),
                "마을 복귀 후 공격력은 변경되지 않아야 한다");
        assertEquals(originalDefense, player.getDefense(),
                "마을 복귀 후 방어력은 변경되지 않아야 한다");
        assertEquals(originalSpeed, player.getSpeed(),
                "마을 복귀 후 속도는 변경되지 않아야 한다");
        assertEquals(originalCritical, player.getCritical(),
                "마을 복귀 후 치명타는 변경되지 않아야 한다");
        assertEquals(originalGold, player.getGold(),
                "마을 복귀 후 골드는 변경되지 않아야 한다");
    }
}
