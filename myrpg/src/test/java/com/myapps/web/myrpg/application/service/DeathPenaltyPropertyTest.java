package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사망 패널티가 올바르게 적용되는지 검증하는 프로퍼티 테스트.
 *
 * <p>최대레벨 미만의 캐릭터에 사망 패널티를 적용할 때,
 * 경험치가 {@code max(0, prev - floor(requiredForNext(level) × 0.10))}으로 설정되고,
 * 레벨·누적레벨·재능은 불변이며, 최대레벨에서는 아무 변경이 없음을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 7: 사망 패널티
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b>
 */
class DeathPenaltyPropertyTest {

    private static final int MAX_LEVEL = 100;
    private static final double DEATH_PENALTY_RATE = 0.10;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 최대레벨 미만 캐릭터에 사망 패널티를 적용할 때 경험치가 올바르게 차감되고
     * 레벨·누적레벨·재능이 불변인지 검증한다.
     *
     * @param level          현재 레벨 (1~99)
     * @param talent         재능 유형
     * @param accumulatedExtra 누적레벨 추가분 (accumulatedLevel = level + extra)
     */
    @Property(tries = 100)
    void should_applyCorrectPenalty_when_notAtMaxLevel(
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent,
            @ForAll("accumulatedExtras") final int accumulatedExtra) {

        // Given: 현재 레벨에서 유효한 범위의 경험치를 가진 캐릭터
        final long required = experiencePolicy.requiredForNext(level);
        final long initialExp = required > 1 ? (long) (required * 0.8) : 0;
        final int accumulatedLevel = level + accumulatedExtra;

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                accumulatedLevel,
                initialExp,
                talent,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0, 0L
        );

        // When: 사망 패널티 적용
        final DeathResult result = progressionService.applyDeathPenalty(progress);

        // Then: 경험치 검증 — exp = max(0, prev - floor(required × 0.10))
        final long expectedLoss = (long) Math.floor(required * DEATH_PENALTY_RATE);
        final long expectedExp = Math.max(0L, initialExp - expectedLoss);

        assertThat(progress.getExperience())
                .as("경험치는 max(0, prev - floor(required×0.10))이어야 한다")
                .isEqualTo(expectedExp);

        // Then: 실제 차감량 검증
        assertThat(result.experienceLost())
                .as("차감량은 초기 경험치 - 최종 경험치여야 한다")
                .isEqualTo(initialExp - progress.getExperience());

        // Then: 레벨 불변
        assertThat(progress.getCurrentLevel())
                .as("레벨은 변하지 않아야 한다")
                .isEqualTo(level);

        // Then: 누적레벨 불변
        assertThat(progress.getAccumulatedLevel())
                .as("누적레벨은 변하지 않아야 한다")
                .isEqualTo(accumulatedLevel);

        // Then: 재능 불변
        assertThat(progress.getTalent())
                .as("재능은 변하지 않아야 한다")
                .isEqualTo(talent);
    }

    /**
     * 경험치가 패널티보다 적을 때 0으로 바닥을 치는지 검증한다.
     *
     * @param level 현재 레벨 (1~99)
     */
    @Property(tries = 100)
    void should_floorToZero_when_expLessThanPenalty(
            @ForAll("levels") final int level) {

        // Given: 패널티보다 적은 경험치 (0~penalty-1)
        final long required = experiencePolicy.requiredForNext(level);
        final long penalty = (long) Math.floor(required * DEATH_PENALTY_RATE);
        final long initialExp = penalty > 0 ? penalty - 1 : 0;

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                level,
                initialExp,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0, 0L
        );

        // When: 사망 패널티 적용
        final DeathResult result = progressionService.applyDeathPenalty(progress);

        // Then: 경험치는 0으로 바닥을 친다
        assertThat(progress.getExperience())
                .as("경험치가 패널티보다 적으면 0이어야 한다")
                .isEqualTo(0L);

        // Then: 차감량은 초기 경험치 전부
        assertThat(result.experienceLost())
                .as("차감량은 초기 경험치 전부여야 한다")
                .isEqualTo(initialExp);
    }

    /**
     * 최대레벨에서 사망 패널티를 적용하면 아무 변경이 없는지 검증한다.
     *
     * @param talent 재능 유형
     */
    @Property(tries = 100)
    void should_noChange_when_atMaxLevel(
            @ForAll("talents") final TalentType talent) {

        // Given: 최대레벨 캐릭터 (exp=0)
        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                MAX_LEVEL,
                MAX_LEVEL + 10,
                0L,
                talent,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0, 0L
        );

        // When: 사망 패널티 적용
        final DeathResult result = progressionService.applyDeathPenalty(progress);

        // Then: 경험치 무변경
        assertThat(progress.getExperience())
                .as("최대레벨에서 경험치는 변하지 않아야 한다")
                .isEqualTo(0L);

        // Then: 차감량 0
        assertThat(result.experienceLost())
                .as("최대레벨에서 차감량은 0이어야 한다")
                .isEqualTo(0L);

        // Then: 레벨 불변
        assertThat(progress.getCurrentLevel())
                .as("최대레벨은 변하지 않아야 한다")
                .isEqualTo(MAX_LEVEL);
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
     * 누적레벨 추가분 생성기: 0~50.
     *
     * @return 추가분 Arbitrary
     */
    @Provide
    Arbitrary<Integer> accumulatedExtras() {
        return Arbitraries.integers().between(0, 50);
    }
}
