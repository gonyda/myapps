package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Feature: myrpg-gen1-mvp, Property 4: 레벨업 종료 불변식
// Feature: myrpg-gen1-mvp, Property 5: 레벨업 시 스탯 증가 및 HP/MP 완충
// Feature: myrpg-gen1-mvp, Property 6: 음수 경험치 거부
/**
 * CharacterService.gainExp 메서드의 레벨업 관련 속성 기반 테스트.
 *
 * <p>레벨업 종료 불변식, 스탯 증가·HP/MP 완충, 음수 경험치 거부를 검증한다.
 *
 * <p><b>Validates: Requirements 2.2, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7</b>
 */
class CharacterServiceLevelUpPropertyTest {

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
    private static final int EXP_AMOUNT_MAX = 100_000;

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
     * 0 이상의 경험치 획득량을 생성하는 Provider.
     *
     * @return 경험치 Arbitrary (0 이상)
     */
    @Provide
    Arbitrary<Integer> nonNegativeExpAmounts() {
        return Arbitraries.integers().between(0, EXP_AMOUNT_MAX);
    }

    /**
     * 0 미만의 경험치 입력을 생성하는 Provider.
     *
     * @return 음수 경험치 Arbitrary
     */
    @Provide
    Arbitrary<Integer> negativeExpAmounts() {
        return Arbitraries.integers().between(Integer.MIN_VALUE, -1);
    }

    // Feature: myrpg-gen1-mvp, Property 4: 레벨업 종료 불변식
    /**
     * gainExp 종료 후 현재 경험치는 0 이상이며 현재 레벨의 필요 경험치 미만이다.
     *
     * <p>필요치를 만족하는 한 반복 레벨업하고, 미만이 되면 멈춘다.
     * 레벨에 하드캡이 없음을 함께 검증한다.
     *
     * <p><b>Validates: Requirements 2.2, 3.2, 3.5, 3.7</b>
     *
     * @param player 테스트 대상 플레이어
     * @param amount 획득 경험치 (0 이상)
     */
    @Property(tries = 100)
    void levelUpTerminationInvariant(
            @ForAll("validPlayers") final Player player,
            @ForAll("nonNegativeExpAmounts") final Integer amount) {

        final int originalLevel = player.getLevel();

        characterService.gainExp(player, amount);

        final int currentExp = player.getExp();
        final int currentLevel = player.getLevel();
        final int required = characterService.requiredExp(currentLevel);

        assertTrue(currentExp >= 0,
                "종료 후 경험치는 0 이상이어야 한다: exp=" + currentExp);
        assertTrue(currentExp < required,
                "종료 후 경험치는 현재 레벨 필요 경험치 미만이어야 한다: exp="
                        + currentExp + ", required=" + required + ", level=" + currentLevel);
        assertTrue(currentLevel >= originalLevel,
                "레벨은 감소하지 않아야 한다: original=" + originalLevel + ", current=" + currentLevel);
    }

    // Feature: myrpg-gen1-mvp, Property 5: 레벨업 시 스탯 증가 및 HP/MP 완충
    /**
     * gainExp 결과 레벨이 k만큼 증가했다면 스탯이 정확히 증가하고 HP/MP가 완충된다.
     *
     * <p>k 레벨 상승 시 maxHp += k×20, maxMp += k×10, attack += k×3,
     * defense += k×2, speed += k×1, critical += k×1.
     * k ≥ 1이면 종료 시 hp == maxHp, mp == maxMp이다.
     *
     * <p><b>Validates: Requirements 3.3, 3.4</b>
     *
     * @param player 테스트 대상 플레이어
     * @param amount 획득 경험치 (0 이상)
     */
    @Property(tries = 100)
    void statIncreaseAndFullRestoreOnLevelUp(
            @ForAll("validPlayers") final Player player,
            @ForAll("nonNegativeExpAmounts") final Integer amount) {

        final int originalLevel = player.getLevel();
        final int originalMaxHp = player.getMaxHp();
        final int originalMaxMp = player.getMaxMp();
        final int originalAttack = player.getAttack();
        final int originalDefense = player.getDefense();
        final int originalSpeed = player.getSpeed();
        final int originalCritical = player.getCritical();

        final LevelUpResult result = characterService.gainExp(player, amount);

        final int levelsGained = result.levelsGained();

        assertEquals(originalMaxHp + levelsGained * HP_PER_LEVEL, player.getMaxHp(),
                "maxHp는 k×20만큼 증가해야 한다: k=" + levelsGained);
        assertEquals(originalMaxMp + levelsGained * MP_PER_LEVEL, player.getMaxMp(),
                "maxMp는 k×10만큼 증가해야 한다: k=" + levelsGained);
        assertEquals(originalAttack + levelsGained * ATTACK_PER_LEVEL, player.getAttack(),
                "attack은 k×3만큼 증가해야 한다: k=" + levelsGained);
        assertEquals(originalDefense + levelsGained * DEFENSE_PER_LEVEL, player.getDefense(),
                "defense는 k×2만큼 증가해야 한다: k=" + levelsGained);
        assertEquals(originalSpeed + levelsGained * SPEED_PER_LEVEL, player.getSpeed(),
                "speed는 k×1만큼 증가해야 한다: k=" + levelsGained);
        assertEquals(originalCritical + levelsGained * CRITICAL_PER_LEVEL, player.getCritical(),
                "critical은 k×1만큼 증가해야 한다: k=" + levelsGained);

        if (levelsGained >= 1) {
            assertEquals(player.getMaxHp(), player.getHp(),
                    "레벨업 시 hp는 maxHp와 같아야 한다");
            assertEquals(player.getMaxMp(), player.getMp(),
                    "레벨업 시 mp는 maxMp와 같아야 한다");
        }
    }

    // Feature: myrpg-gen1-mvp, Property 6: 음수 경험치 거부
    /**
     * gainExp에 음수 경험치를 입력하면 경험치와 레벨이 변경되지 않는다.
     *
     * <p><b>Validates: Requirements 3.6</b>
     *
     * @param player        테스트 대상 플레이어
     * @param negativeAmount 음수 경험치
     */
    @Property(tries = 100)
    void negativeExpIsRejected(
            @ForAll("validPlayers") final Player player,
            @ForAll("negativeExpAmounts") final Integer negativeAmount) {

        final int originalLevel = player.getLevel();
        final int originalExp = player.getExp();

        final LevelUpResult result = characterService.gainExp(player, negativeAmount);

        assertEquals(originalLevel, player.getLevel(),
                "음수 경험치 입력 시 레벨은 변경되지 않아야 한다");
        assertEquals(originalExp, player.getExp(),
                "음수 경험치 입력 시 경험치는 변경되지 않아야 한다");
        assertEquals(0, result.levelsGained(),
                "음수 경험치 입력 시 상승 레벨은 0이어야 한다");
        assertEquals(originalLevel, result.newLevel(),
                "음수 경험치 입력 시 결과 레벨은 원래 레벨이어야 한다");
        assertEquals(originalExp, result.remainingExp(),
                "음수 경험치 입력 시 남은 경험치는 원래 경험치와 같아야 한다");
    }
}
