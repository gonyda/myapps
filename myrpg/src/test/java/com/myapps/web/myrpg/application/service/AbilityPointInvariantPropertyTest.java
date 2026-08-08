package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
 * AP 정합성 불변식을 검증하는 프로퍼티 테스트.
 *
 * <p>소모({@code spendAbilityPoints})가 한 번도 발생하지 않은 진행상황에서,
 * 레벨업과 환생의 임의 시퀀스를 수행한 후
 * {@code abilityPoints == accumulatedLevel - 1}(AP_Invariant)이 항상 성립하는지 검증한다.
 * 또한 신규 생성 직후 {@code 0 == 1 - 1}을 만족하는지 확인한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 2: AP 정합성 불변식
 *
 * <p><b>Validates: Requirements 1.1, 3.1, 14.3</b>
 */
class AbilityPointInvariantPropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 신규 생성 직후 AP 정합성 불변식({@code 0 == 1 - 1})을 검증한다.
     */
    @Property(tries = 100)
    void should_satisfyInvariant_when_createdDefault() {
        // Given: 신규 캐릭터
        final CharacterProgress progress = CharacterProgress.createDefault();

        // Then: AP_Invariant — abilityPoints == accumulatedLevel - 1
        assertThat(progress.getAbilityPoints())
                .as("신규 생성 시 AP는 0이어야 한다")
                .isEqualTo(0);
        assertThat(progress.getAccumulatedLevel())
                .as("신규 생성 시 누적레벨은 1이어야 한다")
                .isEqualTo(1);
        assertThat(progress.getAbilityPoints())
                .as("신규 생성 시 AP == accumulatedLevel - 1 (0 == 1-1)")
                .isEqualTo(progress.getAccumulatedLevel() - 1);
    }

    /**
     * 임의의 레벨업·환생 시퀀스(소모 없음)를 수행한 후
     * AP 정합성 불변식이 매 단계에서 성립하는지 검증한다.
     *
     * <p>시퀀스:
     * <ol>
     *   <li>createDefault() → 불변식 확인</li>
     *   <li>임의 횟수 경험치 획득(레벨업 발생) → 매번 불변식 확인</li>
     *   <li>환생 → 불변식 확인</li>
     *   <li>추가 경험치 획득(레벨업 발생) → 매번 불변식 확인</li>
     * </ol>
     *
     * @param levelUpCountPhase1 1단계 레벨업 횟수 (1~10)
     * @param levelUpCountPhase2 2단계 레벨업 횟수 (1~5)
     * @param rebirthTalent      환생 시 선택할 재능
     */
    @Property(tries = 100)
    void should_maintainInvariant_when_levelUpsAndRebirthsWithoutSpending(
            @ForAll("levelUpCounts") final int levelUpCountPhase1,
            @ForAll("smallLevelUpCounts") final int levelUpCountPhase2,
            @ForAll("talents") final TalentType rebirthTalent) {

        // Given: 신규 캐릭터
        final CharacterProgress progress = CharacterProgress.createDefault();
        assertInvariant(progress, "초기 생성 직후");

        // Phase 1: N번 레벨업 (각각 충분한 경험치 부여)
        for (int i = 0; i < levelUpCountPhase1; i++) {
            final long requiredExp = experiencePolicy.requiredForNext(progress.getCurrentLevel());
            progressionService.gainExperience(progress, requiredExp);
            assertInvariant(progress, "Phase1 레벨업 " + (i + 1) + "회 후");
        }

        // Phase 2: 환생 (lastRebirthAt을 먼 과거로 설정하여 쿨다운 우회)
        progress.setLastRebirthAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        progressionService.rebirth(progress, rebirthTalent);
        assertInvariant(progress, "환생 후");

        // Phase 3: 환생 후 추가 레벨업
        for (int i = 0; i < levelUpCountPhase2; i++) {
            final long requiredExp = experiencePolicy.requiredForNext(progress.getCurrentLevel());
            progressionService.gainExperience(progress, requiredExp);
            assertInvariant(progress, "Phase3 레벨업 " + (i + 1) + "회 후");
        }
    }

    /**
     * AP 정합성 불변식을 검증한다: {@code abilityPoints == accumulatedLevel - 1}.
     *
     * @param progress 검증할 진행상황
     * @param context  실패 시 표시할 맥락 설명
     */
    private void assertInvariant(final CharacterProgress progress, final String context) {
        assertThat(progress.getAbilityPoints())
                .as("AP_Invariant 위반 (%s): AP=%d, accLv=%d",
                        context, progress.getAbilityPoints(), progress.getAccumulatedLevel())
                .isEqualTo(progress.getAccumulatedLevel() - 1);
    }

    /**
     * 1단계 레벨업 횟수 생성기: 1~10.
     *
     * @return 레벨업 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levelUpCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    /**
     * 2단계 레벨업 횟수 생성기: 1~5.
     *
     * @return 레벨업 횟수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> smallLevelUpCounts() {
        return Arbitraries.integers().between(1, 5);
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
