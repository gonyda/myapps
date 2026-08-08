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
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 레벨업/환생 시 HP/MP/Stamina가 바이탈별 최대치로 풀회복되는지 검증하는 프로퍼티 테스트.
 *
 * <p>두 가지 프로퍼티를 검증한다:
 * <ol>
 *   <li>레벨업 풀회복: 임의 재능 T, 레벨 L에서 1회 이상 레벨업 후
 *       HP/MP/Stamina == vitalMaxFor(최종 레벨, T) 각 필드</li>
 *   <li>환생 풀회복: 임의 재능 T로 환생 후
 *       HP/MP/Stamina == vitalMaxFor(1, T) 각 필드</li>
 * </ol>
 *
 * <p>Feature: 004-talent-and-ability-points, Property 11: 레벨업/환생 풀회복 (바이탈별)
 *
 * <p><b>Validates: Requirements 8.3, 8.4, 8.5</b>
 */
class FullRecoveryVitalMaxPropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 레벨업이 1회 이상 발생하면 HP/MP/Stamina가 최종 레벨·재능의 바이탈별 최대치와 같아야 한다.
     *
     * <p>초기 바이탈을 낮은 값(1~30)으로 설정하여 풀회복이 실제로 발생했음을 증명한다.
     * 모든 재능에서 바이탈별 최대치가 대응 필드별로 올바르게 적용되는지 확인한다.
     *
     * @param level       초기 레벨 (1~98, 최소 1회 레벨업 가능)
     * @param talent      캐릭터 재능
     * @param hpCurrent   초기 HP (낮은 값)
     * @param mpCurrent   초기 MP (낮은 값)
     * @param staCurrent  초기 Stamina (낮은 값)
     * @param extraAmount 필요 경험치 초과분
     */
    @Property(tries = 100)
    void should_fullRecoverToVitalMax_when_levelUpOccurs(
            @ForAll("levels") final int level,
            @ForAll("talents") final TalentType talent,
            @ForAll("lowVitals") final int hpCurrent,
            @ForAll("lowVitals") final int mpCurrent,
            @ForAll("lowVitals") final int staCurrent,
            @ForAll("extraAmounts") final long extraAmount) {

        // Given: 바이탈이 낮고 경험치 0인 캐릭터 (재능 T)
        final CharacterProgress progress = new CharacterProgress(
                "테스트캐릭터",
                level,
                level,
                0L,
                talent,
                null,
                hpCurrent,
                mpCurrent,
                staCurrent,
                "tir-chonaill",
                0
        );

        // 최소 1회 레벨업 보장 획득량
        final long amount = experiencePolicy.requiredForNext(level) + extraAmount;

        // When: 경험치 획득 → 레벨업 발생
        final LevelUpResult result = progressionService.gainExperience(progress, amount);

        // Then: 레벨업이 1회 이상 발생
        assertThat(result.levelsGained())
                .as("최소 1회 레벨업이 발생해야 한다")
                .isGreaterThanOrEqualTo(1);

        // Then: HP/MP/Stamina == vitalMaxFor(최종 레벨, talent) 각 필드
        final VitalMax expectedVitalMax = statProgression.vitalMaxFor(result.newLevel(), talent);

        assertThat(progress.getHpCurrent())
                .as("레벨업 풀회복 후 HP는 vitalMaxFor(최종 레벨, talent).hp()와 같아야 한다")
                .isEqualTo(expectedVitalMax.hp());

        assertThat(progress.getMpCurrent())
                .as("레벨업 풀회복 후 MP는 vitalMaxFor(최종 레벨, talent).mp()와 같아야 한다")
                .isEqualTo(expectedVitalMax.mp());

        assertThat(progress.getStaminaCurrent())
                .as("레벨업 풀회복 후 Stamina는 vitalMaxFor(최종 레벨, talent).stamina()와 같아야 한다")
                .isEqualTo(expectedVitalMax.stamina());
    }

    /**
     * 환생 후 HP/MP/Stamina가 선택 재능의 레벨 1 바이탈별 최대치와 같아야 한다.
     *
     * <p>초기 바이탈을 낮은 값(1~30)으로 설정하여 풀회복이 실제로 발생했음을 증명한다.
     * 임의의 현재 재능/레벨에서 임의의 목표 재능으로 환생하여 바이탈별 최대치가 적용되는지 확인한다.
     *
     * @param level         환생 전 현재 레벨
     * @param currentTalent 환생 전 재능
     * @param targetTalent  환생 시 선택할 재능
     * @param hpCurrent     초기 HP (낮은 값)
     * @param mpCurrent     초기 MP (낮은 값)
     * @param staCurrent    초기 Stamina (낮은 값)
     */
    @Property(tries = 100)
    void should_fullRecoverToVitalMax_when_rebirthOccurs(
            @ForAll("rebirthLevels") final int level,
            @ForAll("talents") final TalentType currentTalent,
            @ForAll("talents") final TalentType targetTalent,
            @ForAll("lowVitals") final int hpCurrent,
            @ForAll("lowVitals") final int mpCurrent,
            @ForAll("lowVitals") final int staCurrent) {

        // Given: 환생 가능한 캐릭터 (lastRebirthAt=null → 첫 환생, 항상 가능)
        final CharacterProgress progress = new CharacterProgress(
                "테스트캐릭터",
                level,
                level,
                0L,
                currentTalent,
                null,
                hpCurrent,
                mpCurrent,
                staCurrent,
                "tir-chonaill",
                0
        );

        // When: 선택 재능으로 환생
        final RebirthResult result = progressionService.rebirth(progress, targetTalent);

        // Then: 환생 성공
        assertThat(result)
                .as("환생 가능 상태에서 결과는 Reborn이어야 한다")
                .isInstanceOf(RebirthResult.Reborn.class);

        // Then: HP/MP/Stamina == vitalMaxFor(1, targetTalent) 각 필드
        final VitalMax expectedVitalMax = statProgression.vitalMaxFor(1, targetTalent);

        assertThat(progress.getHpCurrent())
                .as("환생 풀회복 후 HP는 vitalMaxFor(1, targetTalent).hp()와 같아야 한다")
                .isEqualTo(expectedVitalMax.hp());

        assertThat(progress.getMpCurrent())
                .as("환생 풀회복 후 MP는 vitalMaxFor(1, targetTalent).mp()와 같아야 한다")
                .isEqualTo(expectedVitalMax.mp());

        assertThat(progress.getStaminaCurrent())
                .as("환생 풀회복 후 Stamina는 vitalMaxFor(1, targetTalent).stamina()와 같아야 한다")
                .isEqualTo(expectedVitalMax.stamina());
    }

    /**
     * 레벨업 가능한 초기 레벨 생성기: 1~98.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 98);
    }

    /**
     * 환생용 레벨 생성기: 1~100 (환생은 어떤 레벨에서도 가능).
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> rebirthLevels() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 재능 유형 생성기 (3종 모든 재능).
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 낮은 바이탈 값 생성기: 1~30 (풀회복 효과를 명확히 증명).
     *
     * @return 바이탈 현재값 Arbitrary
     */
    @Provide
    Arbitrary<Integer> lowVitals() {
        return Arbitraries.integers().between(1, 30);
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
