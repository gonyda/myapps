package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 환생 쿨다운 판정이 올바르게 동작하는지 검증하는 프로퍼티 테스트.
 *
 * <p>lastRebirthAt 값과 현재 시각의 관계에 따라 환생 가능 여부를 올바르게 판정하고, 쿨다운 활성 상태에서 환생을 시도하면 {@code
 * CooldownActive}를 반환하며 캐릭터 상태가 불변인지 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 9: 환생 쿨다운 판정
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3, 7.4, 10.9</b>
 */
class RebirthCooldownPropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Duration COOLDOWN = Duration.ofHours(24);

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE_ID);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * lastRebirthAt이 null이면 환생 가능(available=true)이고, 환생 이력이 없음(everRebirthed=false)인지 검증한다.
     *
     * @param level 현재 레벨 (1~100)
     */
    @Property(tries = 100)
    void should_beAvailable_when_lastRebirthAtIsNull(@ForAll("levels") final int level) {

        // Given: lastRebirthAt이 null인 캐릭터
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        level,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        // When: 환생 상태 조회
        final RebirthStatus status = progressionService.rebirthStatus(progress);

        // Then: 환생 가능
        assertThat(status.available()).as("lastRebirthAt이 null이면 환생 가능해야 한다").isTrue();

        // Then: 환생 이력 없음
        assertThat(status.everRebirthed()).as("lastRebirthAt이 null이면 환생 이력이 없어야 한다").isFalse();
    }

    /**
     * 마지막 환생으로부터 24시간 이상 경과하면 환생 가능(available=true)인지 검증한다.
     *
     * @param hoursAgo 경과 시간 (24~168시간, 즉 1~7일)
     * @param level 현재 레벨 (1~100)
     */
    @Property(tries = 100)
    void should_beAvailable_when_elapsedIs24HoursOrMore(
            @ForAll("hoursAgoAtLeast24") final int hoursAgo, @ForAll("levels") final int level) {

        // Given: 24시간 이상 전에 환생한 캐릭터
        final LocalDateTime now = LocalDateTime.now(fixedClock);
        final LocalDateTime lastRebirthAt = now.minusHours(hoursAgo);

        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        level,
                        0L,
                        TalentType.MELEE,
                        lastRebirthAt,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        // When: 환생 상태 조회
        final RebirthStatus status = progressionService.rebirthStatus(progress);

        // Then: 환생 가능
        assertThat(status.available()).as("24시간 이상 경과하면 환생 가능해야 한다").isTrue();

        // Then: 환생 이력 있음
        assertThat(status.everRebirthed()).as("lastRebirthAt이 존재하면 환생 이력이 있어야 한다").isTrue();
    }

    /**
     * 마지막 환생으로부터 24시간 미만이면 환생 불가(available=false)이고, 남은 쿨다운 시간이 올바른지 검증한다.
     *
     * @param totalMinutesAgo 경과 시간 (1~1439분, 즉 24시간 미만)
     * @param level 현재 레벨 (1~100)
     */
    @Property(tries = 100)
    void should_notBeAvailable_when_elapsedIsLessThan24Hours(
            @ForAll("minutesAgoLessThan24h") final int totalMinutesAgo,
            @ForAll("levels") final int level) {

        // Given: 24시간 미만 전에 환생한 캐릭터
        final LocalDateTime now = LocalDateTime.now(fixedClock);
        final LocalDateTime lastRebirthAt = now.minusMinutes(totalMinutesAgo);

        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        level,
                        0L,
                        TalentType.MELEE,
                        lastRebirthAt,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        // When: 환생 상태 조회
        final RebirthStatus status = progressionService.rebirthStatus(progress);

        // Then: 환생 불가
        assertThat(status.available()).as("24시간 미만 경과하면 환생 불가해야 한다").isFalse();

        // Then: 남은 쿨다운 시간 검증 (24h - 경과)
        final Duration elapsed = Duration.between(lastRebirthAt, now);
        final Duration expectedRemaining = COOLDOWN.minus(elapsed);

        assertThat(status.remaining()).as("남은 쿨다운은 24h - 경과 시간이어야 한다").isEqualTo(expectedRemaining);
    }

    /**
     * 환생이 불가능한 상태에서 rebirth를 호출하면 CooldownActive가 반환되고, 캐릭터 상태가 전혀 변하지 않는지 검증한다.
     *
     * @param totalMinutesAgo 경과 시간 (1~1439분, 즉 24시간 미만)
     * @param level 현재 레벨 (1~100)
     * @param talent 재능 유형
     */
    @Property(tries = 100)
    void should_returnCooldownActiveAndNoStateChange_when_notAvailable(
            @ForAll("minutesAgoLessThan24h") final int totalMinutesAgo,
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent) {

        // Given: 24시간 미만 전에 환생한 캐릭터 (쿨다운 활성)
        final LocalDateTime now = LocalDateTime.now(fixedClock);
        final LocalDateTime lastRebirthAt = now.minusMinutes(totalMinutesAgo);
        final long initialExp = 500L;
        final int accumulatedLevel = level + 5;
        final int hpCurrent = 80;
        final int mpCurrent = 70;
        final int staminaCurrent = 60;

        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        level,
                        accumulatedLevel,
                        initialExp,
                        talent,
                        lastRebirthAt,
                        hpCurrent,
                        mpCurrent,
                        staminaCurrent,
                        "tir-chonaill",
                        0,
                        0L);

        // When: 환생 시도
        final RebirthResult result = progressionService.rebirth(progress);

        // Then: CooldownActive 반환
        assertThat(result)
                .as("쿨다운 활성 시 CooldownActive가 반환되어야 한다")
                .isInstanceOf(RebirthResult.CooldownActive.class);

        // Then: 남은 쿨다운 시간 검증
        final Duration elapsed = Duration.between(lastRebirthAt, now);
        final Duration expectedRemaining = COOLDOWN.minus(elapsed);
        final RebirthResult.CooldownActive cooldownActive = (RebirthResult.CooldownActive) result;

        assertThat(cooldownActive.remaining())
                .as("CooldownActive의 남은 시간은 24h - 경과 시간이어야 한다")
                .isEqualTo(expectedRemaining);

        // Then: 모든 상태 불변
        assertThat(progress.getCurrentLevel()).as("레벨은 변하지 않아야 한다").isEqualTo(level);
        assertThat(progress.getExperience()).as("경험치는 변하지 않아야 한다").isEqualTo(initialExp);
        assertThat(progress.getAccumulatedLevel())
                .as("누적레벨은 변하지 않아야 한다")
                .isEqualTo(accumulatedLevel);
        assertThat(progress.getTalent()).as("재능은 변하지 않아야 한다").isEqualTo(talent);
        assertThat(progress.getLastRebirthAt()).as("환생 시각은 변하지 않아야 한다").isEqualTo(lastRebirthAt);
        assertThat(progress.getHpCurrent()).as("HP는 변하지 않아야 한다").isEqualTo(hpCurrent);
        assertThat(progress.getMpCurrent()).as("MP는 변하지 않아야 한다").isEqualTo(mpCurrent);
        assertThat(progress.getStaminaCurrent())
                .as("Stamina는 변하지 않아야 한다")
                .isEqualTo(staminaCurrent);
    }

    // ─── Arbitraries ────────────────────────────────────────────────────────

    /**
     * 레벨 생성기: 1~100.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 24시간 이상 경과 시간 생성기: 24~168시간 (1~7일).
     *
     * @return 시간 Arbitrary
     */
    @Provide
    Arbitrary<Integer> hoursAgoAtLeast24() {
        return Arbitraries.integers().between(24, 168);
    }

    /**
     * 24시간 미만 경과 시간 생성기: 1~1439분.
     *
     * @return 분 Arbitrary
     */
    @Provide
    Arbitrary<Integer> minutesAgoLessThan24h() {
        return Arbitraries.integers().between(1, 1439);
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
