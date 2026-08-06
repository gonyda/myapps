package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경험치 획득 시 레벨업과 경험치 보존이 올바르게 수행되는지 검증하는 프로퍼티 테스트.
 *
 * <p>최대레벨 미만의 캐릭터가 임의의 경험치를 획득할 때,
 * 최종 경험치가 다음 레벨 필요치 미만이고(최대레벨 도달 제외),
 * 획득 레벨 수가 실제 증가 레벨과 일치하며,
 * 초기 경험치 + 획득량 = 레벨업 소비 합 + 최종 경험치가 성립함을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 2: 레벨업과 경험치 보존
 *
 * <p><b>Validates: Requirements 2.2, 2.3</b>
 */
class GainExperienceLevelUpPropertyTest {

    private static final int MAX_LEVEL = 100;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 최대레벨 미만 캐릭터에 경험치를 부여할 때 경험치 보존 법칙이 성립하는지 검증한다.
     *
     * <p>1. 최종 exp < requiredForNext(최종 레벨) (최대레벨 도달 시 제외)
     * <p>2. levelsGained == 최종 레벨 - 초기 레벨
     * <p>3. 초기 exp + amount == 레벨업 소비 합 + 최종 exp (최대레벨 도달 시 초과분 폐기 허용)
     *
     * @param initialLevel 초기 레벨 (1~99)
     * @param amount       획득 경험치 (0 이상)
     */
    @Property(tries = 100)
    void should_conserveExperience_when_gainingExperience(
            @ForAll("levels") final int initialLevel,
            @ForAll("amounts") final long amount) {
        // Given: 초기 레벨에서 유효한 경험치를 가진 캐릭터
        final long requiredForInitial = experiencePolicy.requiredForNext(initialLevel);
        final long initialExp = requiredForInitial > 1
                ? (amount % requiredForInitial + requiredForInitial) % requiredForInitial
                : 0;

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                initialLevel,
                initialLevel,
                initialExp,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill"
        );

        // When: 경험치 획득
        final LevelUpResult result = progressionService.gainExperience(progress, amount);

        // Then 1: 최대레벨 미도달 시 최종 exp < 다음 레벨 필요치
        final int finalLevel = progress.getCurrentLevel();
        final long finalExp = progress.getExperience();

        if (finalLevel < MAX_LEVEL) {
            assertThat(finalExp)
                    .as("최종 경험치는 다음 레벨 필요치 미만이어야 한다")
                    .isLessThan(experiencePolicy.requiredForNext(finalLevel));
        }

        // Then 2: 획득 레벨 수 == 최종 레벨 - 초기 레벨
        assertThat(result.levelsGained())
                .as("획득 레벨 수는 레벨 증가분과 일치해야 한다")
                .isEqualTo(finalLevel - initialLevel);

        // Then 3: 경험치 보존 — 초기 exp + amount == 소비 합 + 최종 exp
        //   (최대레벨 도달 시 초과분이 폐기되므로 >= 관계)
        final long consumedSum = computeConsumedExperience(initialLevel, finalLevel);

        if (finalLevel == MAX_LEVEL) {
            // 최대레벨 도달: 초기 exp + amount >= 소비 합 (초과분 폐기)
            assertThat(initialExp + amount)
                    .as("최대레벨 도달 시 초기 exp + amount >= 소비 합이어야 한다")
                    .isGreaterThanOrEqualTo(consumedSum);
            assertThat(finalExp)
                    .as("최대레벨 도달 시 잔여 경험치는 0이어야 한다")
                    .isEqualTo(0L);
        } else {
            // 일반 레벨업: 경험치 정확히 보존
            assertThat(initialExp + amount)
                    .as("경험치 총량이 보존되어야 한다")
                    .isEqualTo(consumedSum + finalExp);
        }
    }

    /**
     * 초기 레벨부터 최종 레벨까지 레벨업에 소비된 경험치의 합을 계산한다.
     *
     * @param fromLevel 시작 레벨 (inclusive)
     * @param toLevel   도달 레벨 (exclusive — 이 레벨에 도달하기 위해 소비)
     * @return 소비된 총 경험치
     */
    private long computeConsumedExperience(final int fromLevel, final int toLevel) {
        long sum = 0;
        for (int level = fromLevel; level < toLevel; level++) {
            sum += experiencePolicy.requiredForNext(level);
        }
        return sum;
    }

    /**
     * 초기 레벨 생성기: 1~99 (최대레벨 미만).
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 99);
    }

    /**
     * 획득 경험치 생성기: 0~5,000,000 (다중 레벨업 포함).
     *
     * @return 획득량 Arbitrary
     */
    @Provide
    Arbitrary<Long> amounts() {
        return Arbitraries.longs().between(0L, 5_000_000L);
    }
}
