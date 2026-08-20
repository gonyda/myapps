package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * {@link ProgressionService#gainExperience}의 최대레벨 캡 규칙을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 진행상황과 획득량에 대해:
 *
 * <ul>
 *   <li>currentLevel은 절대 100을 초과하지 않는다.
 *   <li>currentLevel이 100이면 경험치/레벨이 변하지 않는다(경험치 미누적).
 *   <li>레벨업으로 100에 도달하면 잔여 경험치가 0이 된다.
 * </ul>
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 3: 최대레벨 캡
 *
 * <p><b>Validates: Requirements 1.1, 1.5, 2.4</b>
 */
class MaxLevelCapPropertyTest {

    private static final int MAX_LEVEL = 100;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock clock =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, clock);

    /**
     * 임의의 유효한 캐릭터(레벨 1~99)에 임의의 경험치를 부여해도 최종 currentLevel이 100 이하임을 검증한다.
     *
     * @param levelAndExp 레벨과 경험치 튜플
     * @param amount 획득할 경험치량
     */
    @Property(tries = 100)
    void should_neverExceedMaxLevel_when_gainingExperience(
            @ForAll("belowMaxProgress") final Tuple.Tuple2<Integer, Long> levelAndExp,
            @ForAll("amounts") final long amount) {

        final int level = levelAndExp.get1();
        final long exp = levelAndExp.get2();
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        level,
                        exp,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        progressionService.gainExperience(progress, amount);

        assertThat(progress.getCurrentLevel()).isLessThanOrEqualTo(MAX_LEVEL);
    }

    /**
     * currentLevel이 이미 100인 캐릭터에 임의의 경험치를 부여해도 레벨과 경험치가 전혀 변하지 않음을 검증한다.
     *
     * @param amount 획득할 경험치량
     */
    @Property(tries = 100)
    void should_noChange_when_alreadyAtMaxLevel(@ForAll("amounts") final long amount) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        MAX_LEVEL,
                        MAX_LEVEL,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        final LevelUpResult result = progressionService.gainExperience(progress, amount);

        assertThat(progress.getCurrentLevel()).isEqualTo(MAX_LEVEL);
        assertThat(progress.getExperience()).isEqualTo(0L);
        assertThat(result.levelsGained()).isEqualTo(0);
    }

    /**
     * 레벨업으로 100에 도달한 경우 잔여 경험치가 0임을 검증한다. 레벨 95~99의 캐릭터에게 충분히 큰 경험치를 부여하여 100 도달을 유도한다.
     *
     * @param levelAndExp 레벨(95~99)과 경험치 튜플
     * @param amount 100에 도달하기 충분한 경험치량
     */
    @Property(tries = 100)
    void should_zeroExperience_when_reachingMaxLevel(
            @ForAll("nearMaxProgress") final Tuple.Tuple2<Integer, Long> levelAndExp,
            @ForAll("largeAmounts") final long amount) {

        final int level = levelAndExp.get1();
        final long exp = levelAndExp.get2();
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        level,
                        exp,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        progressionService.gainExperience(progress, amount);

        if (progress.getCurrentLevel() == MAX_LEVEL) {
            assertThat(progress.getExperience()).isEqualTo(0L);
        }
    }

    /**
     * 최대레벨 미만의 캐릭터(레벨 1~99, 유효 경험치)를 생성하는 Arbitrary 제공자.
     *
     * @return (level, experience) 튜플
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Long>> belowMaxProgress() {
        return Arbitraries.integers()
                .between(1, 99)
                .flatMap(
                        level -> {
                            final long required = experiencePolicy.requiredForNext(level);
                            return Arbitraries.longs()
                                    .between(0L, required - 1)
                                    .map(exp -> Tuple.of(level, exp));
                        });
    }

    /**
     * 레벨 95~99의 캐릭터(유효 경험치)를 생성하는 Arbitrary 제공자.
     *
     * @return (level, experience) 튜플
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Long>> nearMaxProgress() {
        return Arbitraries.integers()
                .between(95, 99)
                .flatMap(
                        level -> {
                            final long required = experiencePolicy.requiredForNext(level);
                            return Arbitraries.longs()
                                    .between(0L, required - 1)
                                    .map(exp -> Tuple.of(level, exp));
                        });
    }

    /**
     * 경험치 획득량(0~100,000,000)을 생성하는 Arbitrary 제공자.
     *
     * @return 0 이상 100,000,000 이하의 long 값
     */
    @Provide
    Arbitrary<Long> amounts() {
        return Arbitraries.longs().between(0L, 100_000_000L);
    }

    /**
     * 레벨 100 도달을 유도하기 위한 충분히 큰 경험치량을 생성하는 Arbitrary 제공자.
     *
     * @return 1,000,000 이상 100,000,000 이하의 long 값
     */
    @Provide
    Arbitrary<Long> largeAmounts() {
        return Arbitraries.longs().between(1_000_000L, 100_000_000L);
    }
}
