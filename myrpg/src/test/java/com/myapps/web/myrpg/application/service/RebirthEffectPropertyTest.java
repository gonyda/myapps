package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 환생 성공 시 캐릭터 상태가 올바르게 초기화되는지 검증하는 프로퍼티 테스트.
 *
 * <p>환생 가능한 캐릭터에 대해 {@code rebirth} 호출 후: level=1, exp=0, 누적+1, 재능 MELEE, lastRebirthAt=now, 풀회복,
 * 표시 스탯이 기본값으로 복귀함을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 8: 환생 효과
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.5, 8.6, 9.3</b>
 */
class RebirthEffectPropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 환생 가능한 캐릭터에 환생을 적용할 때 모든 상태가 올바르게 초기화되는지 검증한다.
     *
     * @param level 현재 레벨 (2~100, 환생 효과를 보여주기 위해 1보다 큰 값)
     * @param accExtra 누적레벨 추가분 (accumulatedLevel = level + extra)
     * @param talent 현재 재능 (환생 후 MELEE로 리셋 확인용)
     * @param hpCurrent 현재 HP (낮은 값으로 풀회복 확인)
     * @param mpCurrent 현재 MP
     * @param stamCurrent 현재 Stamina
     */
    @Property(tries = 100)
    void should_resetToBaseState_when_rebirthSucceeds(
            @ForAll("levels") final int level,
            @ForAll("accumulatedExtras") final int accExtra,
            @ForAll("talents") final TalentType talent,
            @ForAll("lowVitals") final int hpCurrent,
            @ForAll("lowVitals") final int mpCurrent,
            @ForAll("lowVitals") final int stamCurrent) {

        // Given: 환생 가능한 캐릭터 (lastRebirthAt=null → 첫 환생, 항상 가능)
        final int accumulatedLevel = level + accExtra;
        final long experience = level < 100 ? experiencePolicy.requiredForNext(level) - 1 : 0L;

        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트캐릭터",
                        level,
                        accumulatedLevel,
                        experience,
                        talent,
                        null,
                        hpCurrent,
                        mpCurrent,
                        stamCurrent,
                        "tir-chonaill",
                        0,
                        0L);

        final int previousAccumulated = progress.getAccumulatedLevel();

        // When: 환생 실행
        final RebirthResult result = progressionService.rebirth(progress);

        // Then: 결과는 Reborn
        assertThat(result)
                .as("환생 가능 상태에서 결과는 Reborn이어야 한다")
                .isInstanceOf(RebirthResult.Reborn.class);

        // Then: currentLevel == 1 (Req 8.1)
        assertThat(progress.getCurrentLevel()).as("환생 후 레벨은 1이어야 한다").isEqualTo(1);

        // Then: experience == 0 (Req 8.1)
        assertThat(progress.getExperience()).as("환생 후 경험치는 0이어야 한다").isEqualTo(0L);

        // Then: accumulatedLevel == previous + 1 (Req 8.2)
        assertThat(progress.getAccumulatedLevel())
                .as("환생 후 누적레벨은 이전 대비 +1이어야 한다")
                .isEqualTo(previousAccumulated + 1);

        // Then: talent == MELEE (Req 8.6, 9.3)
        assertThat(progress.getTalent()).as("환생 후 재능은 MELEE여야 한다").isEqualTo(TalentType.MELEE);

        // Then: lastRebirthAt == LocalDateTime.now(clock) (Req 8.3)
        final LocalDateTime expectedRebirthAt = LocalDateTime.now(fixedClock);
        assertThat(progress.getLastRebirthAt())
                .as("환생 후 lastRebirthAt은 현재 시각이어야 한다")
                .isEqualTo(expectedRebirthAt);

        // Then: 풀회복 — HP/MP/Stamina == vitalMaxFor(1) == 100 (Req 8.5)
        final int expectedVital = statProgression.vitalMaxFor(1);
        assertThat(progress.getHpCurrent()).as("환생 후 HP는 레벨 1 최대치여야 한다").isEqualTo(expectedVital);
        assertThat(progress.getMpCurrent()).as("환생 후 MP는 레벨 1 최대치여야 한다").isEqualTo(expectedVital);
        assertThat(progress.getStaminaCurrent())
                .as("환생 후 Stamina는 레벨 1 최대치여야 한다")
                .isEqualTo(expectedVital);

        // Then: 표시 스탯이 기본값으로 복귀 (Req 8.3)
        final Stats baseStats = statProgression.levelStatsFor(1);
        assertThat(baseStats.str()).as("레벨 1 STR은 기본값 10이어야 한다").isEqualTo(10);
        assertThat(baseStats.dex()).as("레벨 1 DEX는 기본값 10이어야 한다").isEqualTo(10);
        assertThat(baseStats.intelligence()).as("레벨 1 INT는 기본값 10이어야 한다").isEqualTo(10);
        assertThat(baseStats.critical()).as("레벨 1 Critical은 기본값 50이어야 한다").isEqualTo(50);
        assertThat(baseStats.defense()).as("레벨 1 DEF는 기본값 5이어야 한다").isEqualTo(5);
    }

    /**
     * 레벨 생성기: 2~100 (1보다 큰 값으로 환생 리셋 효과를 보여줌).
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(2, 100);
    }

    /**
     * 누적레벨 추가분 생성기: 0~100.
     *
     * @return 추가분 Arbitrary
     */
    @Provide
    Arbitrary<Integer> accumulatedExtras() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * 재능 유형 생성기 (모든 재능 — 환생 후 MELEE 리셋 확인).
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 낮은 바이탈 생성기: 1~50 (풀회복 효과 확인용).
     *
     * @return 바이탈 현재값 Arbitrary
     */
    @Provide
    Arbitrary<Integer> lowVitals() {
        return Arbitraries.integers().between(1, 50);
    }
}
