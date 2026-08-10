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
 * AP 지급과 누적레벨 동기를 검증하는 프로퍼티 테스트.
 *
 * <p>레벨업·환생 임의 시퀀스에서 AP 증가량 = 누적레벨 증가량이 항상 성립하며,
 * 최대레벨(100)에서는 경험치를 획득해도 둘 다 증가하지 않음을 검증한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 1: AP 지급과 누적레벨 동기
 *
 * <p><b>Validates: Requirements 1.2, 1.3, 1.4, 1.6, 3.2, 3.3</b>
 */
class AbilityPointGrantPropertyTest {

    private static final int MAX_LEVEL = 100;
    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 레벨업과 환생의 임의 시퀀스에서 AP 증가량이 누적레벨 증가량과 동일한지 검증한다.
     *
     * <p>임의의 레벨업 횟수 N과 환생 횟수 M에 대해:
     * N회 레벨업 후 AP += N, 누적레벨 += N,
     * M회 환생 후 AP += M, 누적레벨 += M이 성립한다.
     * 따라서 전체 시퀀스 후 deltaAP == deltaAccLevel.
     *
     * @param levelUps 레벨업 횟수 (1~20)
     * @param rebirths 환생 횟수 (0~5)
     * @param talent   환생 시 선택 재능
     */
    @Property(tries = 100)
    void should_syncApWithAccumulatedLevel_when_levelUpAndRebirthSequence(
            @ForAll("levelUpCounts") final int levelUps,
            @ForAll("rebirthCounts") final int rebirths,
            @ForAll("talents") final TalentType talent) {

        // Given: 신규 캐릭터 (level 1, AP 0, accLevel 1)
        final CharacterProgress progress = CharacterProgress.createDefault();
        final int initialAp = progress.getAbilityPoints();
        final int initialAccLevel = progress.getAccumulatedLevel();

        // When: N회 레벨업
        int levelUpsDone = 0;
        for (int i = 0; i < levelUps; i++) {
            if (progress.getCurrentLevel() >= MAX_LEVEL) {
                break;
            }
            final long required = experiencePolicy.requiredForNext(progress.getCurrentLevel());
            progressionService.gainExperience(progress, required);
            levelUpsDone++;
        }

        // When: M회 환생 (각 환생 사이에 lastRebirthAt=null 유지를 위해 직접 null 설정)
        int rebirthsDone = 0;
        for (int i = 0; i < rebirths; i++) {
            progress.setLastRebirthAt(null);
            progressionService.rebirth(progress, talent);
            rebirthsDone++;
        }

        // Then: AP 증가량 == 누적레벨 증가량
        final int deltaAp = progress.getAbilityPoints() - initialAp;
        final int deltaAccLevel = progress.getAccumulatedLevel() - initialAccLevel;

        assertThat(deltaAp)
                .as("AP 증가량은 누적레벨 증가량과 같아야 한다 (레벨업 %d회 + 환생 %d회)",
                        levelUpsDone, rebirthsDone)
                .isEqualTo(deltaAccLevel);
    }

    /**
     * 최대레벨(100)에서 경험치를 획득해도 AP와 누적레벨이 증가하지 않음을 검증한다.
     *
     * @param extraExp 최대레벨에서 추가 획득하는 경험치량
     */
    @Property(tries = 100)
    void should_notIncreaseApOrAccLevel_when_atMaxLevel(
            @ForAll("extraExpAmounts") final long extraExp) {

        // Given: 최대레벨 캐릭터
        final int accumulatedLevel = MAX_LEVEL + 5;
        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                MAX_LEVEL,
                accumulatedLevel,
                0L,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                accumulatedLevel - 1, 0L
        );

        final int apBefore = progress.getAbilityPoints();
        final int accLevelBefore = progress.getAccumulatedLevel();

        // When: 경험치 획득 시도
        progressionService.gainExperience(progress, extraExp);

        // Then: AP와 누적레벨 모두 불변
        assertThat(progress.getAbilityPoints())
                .as("최대레벨에서 AP는 변하지 않아야 한다")
                .isEqualTo(apBefore);
        assertThat(progress.getAccumulatedLevel())
                .as("최대레벨에서 누적레벨은 변하지 않아야 한다")
                .isEqualTo(accLevelBefore);
    }

    /**
     * 레벨업 횟수 생성기: 1~20.
     *
     * @return 레벨업 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levelUpCounts() {
        return Arbitraries.integers().between(1, 20);
    }

    /**
     * 환생 횟수 생성기: 0~5.
     *
     * @return 환생 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> rebirthCounts() {
        return Arbitraries.integers().between(0, 5);
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
     * 최대레벨 추가 경험치 생성기: 1~1,000,000.
     *
     * @return 경험치량 Arbitrary
     */
    @Provide
    Arbitrary<Long> extraExpAmounts() {
        return Arbitraries.longs().between(1L, 1_000_000L);
    }
}
