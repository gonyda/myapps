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

import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 환생 쿨다운 재검증 프로퍼티 테스트.
 *
 * <p>{@code rebirthStatus(p).available() == false}인 상태에서
 * {@code rebirth(p, T)}를 호출하면 {@link RebirthResult.CooldownActive}를 반환하고,
 * 재능을 포함한 모든 캐릭터 상태가 불변인지 검증한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 12: 환생 쿨다운 재검증
 *
 * <p><b>Validates: Requirements 5.9</b>
 */
class RebirthCooldownRevalidatePropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE_ID);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 쿨다운 활성(available==false) 상태에서 rebirth(p, T)를 호출하면
     * CooldownActive를 반환하고, 재능을 포함한 모든 필드가 불변인지 검증한다.
     *
     * @param hoursAgo       마지막 환생으로부터 경과 시간 (1~23시간, 쿨다운 미충족)
     * @param level          현재 레벨 (1~100)
     * @param currentTalent  현재 보유 재능
     * @param targetTalent   환생 시 선택하려는 재능
     * @param abilityPoints  보유 AP (0~100)
     * @param hpCurrent      HP 현재값 (1~200)
     * @param mpCurrent      MP 현재값 (1~200)
     * @param staminaCurrent Stamina 현재값 (1~200)
     */
    @Property(tries = 100)
    void should_returnCooldownActiveAndPreserveAllFields_when_cooldownNotMet(
            @ForAll("hoursWithinCooldown") final int hoursAgo,
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType currentTalent,
            @ForAll("talents") final TalentType targetTalent,
            @ForAll("abilityPointValues") final int abilityPoints,
            @ForAll("vitalValues") final int hpCurrent,
            @ForAll("vitalValues") final int mpCurrent,
            @ForAll("vitalValues") final int staminaCurrent) {

        // Given: 쿨다운 미충족 캐릭터 (1~23시간 전 환생)
        final LocalDateTime now = LocalDateTime.now(fixedClock);
        final LocalDateTime lastRebirthAt = now.minusHours(hoursAgo);
        final int accumulatedLevel = level + 5;
        final long experience = 500L;

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                accumulatedLevel,
                experience,
                currentTalent,
                lastRebirthAt,
                hpCurrent,
                mpCurrent,
                staminaCurrent,
                "tir-chonaill",
                abilityPoints, 0L
        );

        // Precondition: available == false 확인
        assertThat(progressionService.rebirthStatus(progress).available())
                .as("전제조건: 쿨다운 활성 상태여야 한다")
                .isFalse();

        // When: 임의의 재능 T로 환생 시도
        final RebirthResult result = progressionService.rebirth(progress, targetTalent);

        // Then: CooldownActive 반환
        assertThat(result)
                .as("쿨다운 활성 시 CooldownActive가 반환되어야 한다")
                .isInstanceOf(RebirthResult.CooldownActive.class);

        // Then: 재능 포함 모든 상태 불변
        assertThat(progress.getTalent())
                .as("재능은 변하지 않아야 한다")
                .isEqualTo(currentTalent);
        assertThat(progress.getCurrentLevel())
                .as("레벨은 변하지 않아야 한다")
                .isEqualTo(level);
        assertThat(progress.getExperience())
                .as("경험치는 변하지 않아야 한다")
                .isEqualTo(experience);
        assertThat(progress.getAccumulatedLevel())
                .as("누적레벨은 변하지 않아야 한다")
                .isEqualTo(accumulatedLevel);
        assertThat(progress.getAbilityPoints())
                .as("AP는 변하지 않아야 한다")
                .isEqualTo(abilityPoints);
        assertThat(progress.getHpCurrent())
                .as("HP는 변하지 않아야 한다")
                .isEqualTo(hpCurrent);
        assertThat(progress.getMpCurrent())
                .as("MP는 변하지 않아야 한다")
                .isEqualTo(mpCurrent);
        assertThat(progress.getStaminaCurrent())
                .as("Stamina는 변하지 않아야 한다")
                .isEqualTo(staminaCurrent);
        assertThat(progress.getLastRebirthAt())
                .as("환생 시각은 변하지 않아야 한다")
                .isEqualTo(lastRebirthAt);
    }

    // ─── Arbitraries ────────────────────────────────────────────────────────

    /**
     * 쿨다운 미충족 시간 생성기: 1~23시간 (24시간 미만).
     *
     * @return 시간 Arbitrary
     */
    @Provide
    Arbitrary<Integer> hoursWithinCooldown() {
        return Arbitraries.integers().between(1, 23);
    }

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
     * 재능 유형 생성기.
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 어빌리티 포인트 생성기: 0~100.
     *
     * @return AP Arbitrary
     */
    @Provide
    Arbitrary<Integer> abilityPointValues() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * 바이탈 현재값 생성기: 1~200.
     *
     * @return 바이탈 Arbitrary
     */
    @Provide
    Arbitrary<Integer> vitalValues() {
        return Arbitraries.integers().between(1, 200);
    }
}
