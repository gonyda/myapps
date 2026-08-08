package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 레벨업이 1회 이상 발생했을 때 HP/MP/Stamina가 최종 레벨·재능의 바이탈별 최대치로 풀회복되는지 검증하는 프로퍼티 테스트.
 *
 * <p>현재 바이탈을 낮은 값으로 설정한 뒤 경험치를 부여하여 레벨업을 유발하고,
 * 레벨업 후 HP/MP/Stamina 현재치가 {@code vitalMaxFor(최종 레벨, talent)}의 각 대응 필드와 같음을 확인한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 6: 레벨업 시 풀회복
 *
 * <p><b>Validates: Requirements 3.3, 3.4</b>
 */
class LevelUpFullRecoveryPropertyTest {

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 1회 이상 레벨업 시 HP/MP/Stamina 현재치가 최종 레벨의 최대 바이탈과 같아야 한다.
     *
     * <p>바이탈을 낮은 값(1~50)으로 설정하여 회복이 확실히 일어났음을 증명한다.
     * 획득 경험치는 최소 1회 레벨업을 보장하도록 생성한다.
     *
     * @param level       초기 레벨 (1~98, 최소 1회 레벨업 가능)
     * @param hpCurrent   초기 HP 현재값 (낮은 값)
     * @param mpCurrent   초기 MP 현재값 (낮은 값)
     * @param staCurrent  초기 Stamina 현재값 (낮은 값)
     * @param extraAmount 필요 경험치 초과분 (0 이상)
     */
    @Property(tries = 100)
    void should_fullRecoverVitals_when_levelUpOccurs(
            @ForAll("levels") final int level,
            @ForAll("lowVitals") final int hpCurrent,
            @ForAll("lowVitals") final int mpCurrent,
            @ForAll("lowVitals") final int staCurrent,
            @ForAll("extraAmounts") final long extraAmount) {

        // Given: 레벨업 전 바이탈이 낮고, 경험치는 0인 캐릭터
        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                level,
                0L,
                TalentType.MELEE,
                null,
                hpCurrent,
                mpCurrent,
                staCurrent,
                "tir-chonaill",
                0
        );

        // 최소 1회 레벨업을 보장하는 획득량: requiredForNext(level) + extraAmount
        final long amount = experiencePolicy.requiredForNext(level) + extraAmount;

        // When: 경험치 획득 (레벨업 발생)
        final LevelUpResult result = progressionService.gainExperience(progress, amount);

        // Then: 레벨업이 1회 이상 발생했으므로 풀회복이 적용됨
        assertThat(result.levelsGained())
                .as("최소 1회 레벨업이 발생해야 한다")
                .isGreaterThanOrEqualTo(1);

        final VitalMax expectedVitalMax = statProgression.vitalMaxFor(result.newLevel(), TalentType.MELEE);

        assertThat(progress.getHpCurrent())
                .as("레벨업 후 HP 현재치는 최종 레벨·재능의 HP 최대치와 같아야 한다")
                .isEqualTo(expectedVitalMax.hp());

        assertThat(progress.getMpCurrent())
                .as("레벨업 후 MP 현재치는 최종 레벨·재능의 MP 최대치와 같아야 한다")
                .isEqualTo(expectedVitalMax.mp());

        assertThat(progress.getStaminaCurrent())
                .as("레벨업 후 Stamina 현재치는 최종 레벨·재능의 Stamina 최대치와 같아야 한다")
                .isEqualTo(expectedVitalMax.stamina());
    }

    /**
     * 초기 레벨 생성기: 1~98 (최소 1회 레벨업 가능하도록 99 미만).
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 98);
    }

    /**
     * 낮은 바이탈 값 생성기: 1~50 (풀회복 효과를 명확히 증명).
     *
     * @return 바이탈 현재값 Arbitrary
     */
    @Provide
    Arbitrary<Integer> lowVitals() {
        return Arbitraries.integers().between(1, 50);
    }

    /**
     * 추가 경험치 생성기: 0~500,000 (다중 레벨업 포함 가능).
     *
     * @return 추가 획득량 Arbitrary
     */
    @Provide
    Arbitrary<Long> extraAmounts() {
        return Arbitraries.longs().between(0L, 500_000L);
    }
}
