package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Monster}의 방어 상수({@code defenseBlockRate}, {@code defenseCounterRate}) 기본값을 검증하는 단위 테스트.
 *
 * <p>12파라미터 보조 생성자로 생성 시 전역 기본값(경감 40% / 반격 30%)이 적용되고,
 * 14파라미터 정규 생성자로 명시 시 오버라이드 값이 사용됨을 확인한다.
 *
 * <p><b>Validates: Requirements 22.1</b>
 */
class MonsterDefenseConstantTest {

    private static final String TEST_ID = "test-monster";
    private static final String TEST_NAME = "테스트 몬스터";
    private static final int TEST_LEVEL = 5;
    private static final int TEST_MAX_HP = 100;
    private static final int TEST_ATTACK_POWER = 20;
    private static final int TEST_DEFENSE = 10;
    private static final int TEST_CRITICAL = 50;
    private static final long TEST_EXPERIENCE = 30L;
    private static final int DEFAULT_BLOCK_RATE = 40;
    private static final int DEFAULT_COUNTER_RATE = 30;

    private static final GoldDrop TEST_GOLD_DROP = new GoldDrop(5, 15);
    private static final List<ItemDrop> TEST_ITEM_DROPS = List.of();
    private static final List<String> TEST_LINES = List.of("끼익!", "너구리가 경계한다.", "너구리가 발톱을 세운다.");

    /**
     * 12파라미터 보조 생성자로 생성 시 defenseBlockRate가 전역 기본값 40을 반환하는지 검증한다.
     */
    @Test
    void should_returnDefaultBlockRate_when_createdWith12ParamConstructor() {
        final Monster monster = new Monster(
                TEST_ID, TEST_NAME, MonsterType.NORMAL,
                TEST_LEVEL, TEST_MAX_HP, TEST_ATTACK_POWER,
                TEST_DEFENSE, TEST_CRITICAL, TEST_EXPERIENCE,
                TEST_GOLD_DROP, TEST_ITEM_DROPS, TEST_LINES
        );

        assertThat(monster.defenseBlockRate()).isEqualTo(DEFAULT_BLOCK_RATE);
    }

    /**
     * 12파라미터 보조 생성자로 생성 시 defenseCounterRate가 전역 기본값 30을 반환하는지 검증한다.
     */
    @Test
    void should_returnDefaultCounterRate_when_createdWith12ParamConstructor() {
        final Monster monster = new Monster(
                TEST_ID, TEST_NAME, MonsterType.NORMAL,
                TEST_LEVEL, TEST_MAX_HP, TEST_ATTACK_POWER,
                TEST_DEFENSE, TEST_CRITICAL, TEST_EXPERIENCE,
                TEST_GOLD_DROP, TEST_ITEM_DROPS, TEST_LINES
        );

        assertThat(monster.defenseCounterRate()).isEqualTo(DEFAULT_COUNTER_RATE);
    }

    /**
     * 14파라미터 정규 생성자로 명시된 방어 상수(60/50)가 그대로 반환되는지 검증한다.
     */
    @Test
    void should_returnOverrideValues_when_createdWith14ParamConstructor() {
        final int customBlockRate = 60;
        final int customCounterRate = 50;

        final Monster monster = new Monster(
                TEST_ID, TEST_NAME, MonsterType.BOSS,
                TEST_LEVEL, TEST_MAX_HP, TEST_ATTACK_POWER,
                TEST_DEFENSE, TEST_CRITICAL, TEST_EXPERIENCE,
                TEST_GOLD_DROP, TEST_ITEM_DROPS, TEST_LINES,
                customBlockRate, customCounterRate
        );

        assertThat(monster.defenseBlockRate()).isEqualTo(customBlockRate);
        assertThat(monster.defenseCounterRate()).isEqualTo(customCounterRate);
    }

    /**
     * 14파라미터 정규 생성자로 방어 상수를 0/0으로 명시 시 정확히 0이 반환되는지 검증한다.
     */
    @Test
    void should_returnZeroValues_when_createdWith14ParamConstructorWithZeros() {
        final Monster monster = new Monster(
                TEST_ID, TEST_NAME, MonsterType.NORMAL,
                TEST_LEVEL, TEST_MAX_HP, TEST_ATTACK_POWER,
                TEST_DEFENSE, TEST_CRITICAL, TEST_EXPERIENCE,
                TEST_GOLD_DROP, TEST_ITEM_DROPS, TEST_LINES,
                0, 0
        );

        assertThat(monster.defenseBlockRate()).isEqualTo(0);
        assertThat(monster.defenseCounterRate()).isEqualTo(0);
    }
}
