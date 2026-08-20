package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 사망 처리({@link ProgressionService#die}) 불변식을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 캐릭터 상태에서 {@code die(progress)}를 호출한 뒤, 경험치 -10% 적용·HP/MP/스태미나 풀 회복·티르코네일 이동·골드/아이템 불변 불변식이
 * 성립하는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 12: 사망 처리 불변식
 *
 * <p><b>Validates: Requirements 11.3, 11.4, 11.5</b>
 */
class ProgressionDeathPropertyTest {

    private static final int MAX_LEVEL = 100;
    private static final double DEATH_PENALTY_RATE = 0.10;
    private static final String RESPAWN_NODE_ID = "tir-chonaill";

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * die() 호출 후 경험치가 applyDeathPenalty와 동일하게 -10% 적용되는지 검증한다.
     *
     * @param level 현재 레벨 (1~99)
     * @param talent 재능 유형
     * @param gold 보유 골드 (0~100000)
     */
    @Property(tries = 100)
    void should_reduceExperienceByTenPercent_when_die(
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent,
            @ForAll("golds") final long gold) {

        final long required = experiencePolicy.requiredForNext(level);
        final long initialExp = (long) (required * 0.7);

        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        level,
                        level + 5,
                        initialExp,
                        talent,
                        null,
                        50,
                        30,
                        40,
                        "dunbarton",
                        3,
                        gold);

        final DeathResult result = progressionService.die(progress);

        final long expectedLoss = (long) Math.floor(required * DEATH_PENALTY_RATE);
        final long expectedExp = Math.max(0L, initialExp - expectedLoss);

        assertThat(progress.getExperience())
                .as("경험치는 max(0, prev - floor(required×0.10))이어야 한다")
                .isEqualTo(expectedExp);
        assertThat(result.experienceLost())
                .as("차감량은 초기-최종 경험치여야 한다")
                .isEqualTo(initialExp - expectedExp);
    }

    /**
     * die() 호출 후 HP/MP/스태미나가 해당 레벨·재능의 최대치로 완전 회복되는지 검증한다.
     *
     * @param level 현재 레벨 (1~99)
     * @param talent 재능 유형
     */
    @Property(tries = 100)
    void should_fullRecoverVitals_when_die(
            @ForAll("levels") final int level, @ForAll("talents") final TalentType talent) {

        final long required = experiencePolicy.requiredForNext(level);
        final long initialExp = required > 1 ? required / 2 : 0;

        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        level,
                        level + 2,
                        initialExp,
                        talent,
                        null,
                        1,
                        1,
                        1,
                        "dugald-aisle",
                        0,
                        500L);

        progressionService.die(progress);

        final VitalMax expectedVitalMax = statProgression.vitalMaxFor(level, talent);

        assertThat(progress.getHpCurrent())
                .as("HP는 레벨·재능 기반 최대치여야 한다")
                .isEqualTo(expectedVitalMax.hp());
        assertThat(progress.getMpCurrent())
                .as("MP는 레벨·재능 기반 최대치여야 한다")
                .isEqualTo(expectedVitalMax.mp());
        assertThat(progress.getStaminaCurrent())
                .as("스태미나는 레벨·재능 기반 최대치여야 한다")
                .isEqualTo(expectedVitalMax.stamina());
    }

    /**
     * die() 호출 후 currentNodeId가 "tir-chonaill"로 변경되는지 검증한다.
     *
     * @param level 현재 레벨 (1~99)
     * @param talent 재능 유형
     * @param startNode 사망 전 위치 노드 ID
     */
    @Property(tries = 100)
    void should_respawnAtTirChonaill_when_die(
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent,
            @ForAll("nodeIds") final String startNode) {

        final long required = experiencePolicy.requiredForNext(level);
        final long initialExp = required > 1 ? required / 3 : 0;

        final CharacterProgress progress =
                new CharacterProgress(
                        "전사", level, level, initialExp, talent, null, 10, 10, 10, startNode, 0, 0L);

        progressionService.die(progress);

        assertThat(progress.getCurrentNodeId())
                .as("사망 후 currentNodeId는 tir-chonaill이어야 한다")
                .isEqualTo(RESPAWN_NODE_ID);
    }

    /**
     * die() 호출 후 골드가 불변인지 검증한다.
     *
     * @param level 현재 레벨 (1~99)
     * @param talent 재능 유형
     * @param gold 보유 골드 (0~100000)
     */
    @Property(tries = 100)
    void should_preserveGold_when_die(
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent,
            @ForAll("golds") final long gold) {

        final long required = experiencePolicy.requiredForNext(level);
        final long initialExp = required > 1 ? required / 4 : 0;

        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        level,
                        level + 1,
                        initialExp,
                        talent,
                        null,
                        20,
                        20,
                        20,
                        "bangor",
                        2,
                        gold);

        progressionService.die(progress);

        assertThat(progress.getGold()).as("사망 후 골드는 불변이어야 한다").isEqualTo(gold);
    }

    /**
     * 최대레벨에서 die() 호출 시 경험치 변동 없이 풀 회복·리스폰만 수행하는지 검증한다.
     *
     * @param talent 재능 유형
     * @param gold 보유 골드 (0~100000)
     */
    @Property(tries = 100)
    void should_noExpChange_when_dieAtMaxLevel(
            @ForAll("talents") final TalentType talent, @ForAll("golds") final long gold) {

        final CharacterProgress progress =
                new CharacterProgress(
                        "전사",
                        MAX_LEVEL,
                        MAX_LEVEL + 20,
                        0L,
                        talent,
                        null,
                        5,
                        5,
                        5,
                        "dunbarton",
                        10,
                        gold);

        final DeathResult result = progressionService.die(progress);

        assertThat(progress.getExperience()).as("최대레벨에서 경험치는 불변이어야 한다").isEqualTo(0L);
        assertThat(result.experienceLost()).as("최대레벨에서 차감량은 0이어야 한다").isEqualTo(0L);
        assertThat(progress.getCurrentNodeId())
                .as("최대레벨이라도 리스폰은 수행되어야 한다")
                .isEqualTo(RESPAWN_NODE_ID);

        final VitalMax expectedVitalMax = statProgression.vitalMaxFor(MAX_LEVEL, talent);
        assertThat(progress.getHpCurrent())
                .as("최대레벨에서도 HP 풀 회복이어야 한다")
                .isEqualTo(expectedVitalMax.hp());
        assertThat(progress.getGold()).as("최대레벨에서도 골드는 불변이어야 한다").isEqualTo(gold);
    }

    /**
     * 레벨 생성기: 1~99 (최대레벨 미만).
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 99);
    }

    /**
     * 재능 유형 생성기.
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 골드 생성기: 0~100000.
     *
     * @return 골드 Arbitrary
     */
    @Provide
    Arbitrary<Long> golds() {
        return Arbitraries.longs().between(0L, 100000L);
    }

    /**
     * 노드 ID 생성기: 다양한 시작 위치.
     *
     * @return 노드 ID Arbitrary
     */
    @Provide
    Arbitrary<String> nodeIds() {
        return Arbitraries.of(
                "tir-chonaill", "dunbarton", "bangor", "dugald-aisle", "math-dungeon-lobby");
    }
}
