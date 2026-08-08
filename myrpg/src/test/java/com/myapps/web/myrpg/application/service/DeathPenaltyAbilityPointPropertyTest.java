package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code applyDeathPenalty} 후 어빌리티 포인트가 불변임을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 레벨(1~100), 임의의 AP(0~200), 임의의 경험치, 임의의 재능을 가진
 * 캐릭터에 사망 패널티를 적용해도 보유 AP는 변하지 않아야 한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 3: 사망 패널티 AP 불변
 *
 * <p><b>Validates: Requirements 1.5</b>
 */
class DeathPenaltyAbilityPointPropertyTest {

    private static final int MAX_LEVEL = 100;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 레벨 1~99 캐릭터에 사망 패널티를 적용해도 AP가 변하지 않는지 검증한다.
     *
     * @param level        현재 레벨 (1~99)
     * @param abilityPoints 보유 AP (0~200)
     * @param talent       재능 유형
     */
    @Property(tries = 100)
    void should_keepAbilityPointsUnchanged_when_deathPenaltyApplied(
            @ForAll("levelsBelow100") final int level,
            @ForAll("abilityPointsRange") final int abilityPoints,
            @ForAll("talents") final TalentType talent) {

        // Given: 임의의 AP와 경험치를 가진 캐릭터
        final long required = experiencePolicy.requiredForNext(level);
        final long experience = required > 1 ? required - 1 : 0;

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                level + 10,
                experience,
                talent,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                abilityPoints
        );

        final int apBefore = progress.getAbilityPoints();

        // When: 사망 패널티 적용
        progressionService.applyDeathPenalty(progress);

        // Then: AP는 변하지 않아야 한다
        assertThat(progress.getAbilityPoints())
                .as("사망 패널티 후 AP는 불변이어야 한다")
                .isEqualTo(apBefore);
    }

    /**
     * 최대레벨(100)에서 사망 패널티를 적용해도 AP가 변하지 않는지 검증한다.
     *
     * @param abilityPoints 보유 AP (0~200)
     * @param talent        재능 유형
     */
    @Property(tries = 100)
    void should_keepAbilityPointsUnchanged_when_atMaxLevel(
            @ForAll("abilityPointsRange") final int abilityPoints,
            @ForAll("talents") final TalentType talent) {

        // Given: 최대레벨 캐릭터
        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                MAX_LEVEL,
                MAX_LEVEL + 20,
                0L,
                talent,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                abilityPoints
        );

        final int apBefore = progress.getAbilityPoints();

        // When: 사망 패널티 적용
        progressionService.applyDeathPenalty(progress);

        // Then: AP는 변하지 않아야 한다
        assertThat(progress.getAbilityPoints())
                .as("최대레벨에서 사망 패널티 후 AP는 불변이어야 한다")
                .isEqualTo(apBefore);
    }

    /**
     * 레벨 1~99 생성기.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levelsBelow100() {
        return Arbitraries.integers().between(1, 99);
    }

    /**
     * 보유 AP 생성기: 0~200.
     *
     * @return AP Arbitrary
     */
    @Provide
    Arbitrary<Integer> abilityPointsRange() {
        return Arbitraries.integers().between(0, 200);
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
}
