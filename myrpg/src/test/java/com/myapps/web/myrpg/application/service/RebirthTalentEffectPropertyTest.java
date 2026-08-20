package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 환생 시 선택 재능이 올바르게 반영되고 AP가 지급되는지 검증하는 프로퍼티 테스트.
 *
 * <p>환생 가능한 캐릭터에 대해 {@code rebirth(p, T)} 호출 후: talent==T, level=1, exp=0, 누적+1, AP+1,
 * HP/MP/Stamina 현재값이 {@code vitalMaxFor(1, T)}의 각 필드와 같음을 검증한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 9: 환생 재능 반영과 AP 지급
 *
 * <p><b>Validates: Requirements 12.1, 12.2, 12.3, 3.3, 1.4</b>
 */
class RebirthTalentEffectPropertyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final ProgressionService progressionService =
            new ProgressionService(experiencePolicy, statProgression, fixedClock);

    /**
     * 환생 가능한 캐릭터에 선택 재능으로 환생 시 모든 필드가 올바르게 설정되는지 검증한다.
     *
     * <p>rebirth(p, T) 후:
     *
     * <ul>
     *   <li>talent == T (Req 12.1)
     *   <li>currentLevel == 1 (Req 12.2)
     *   <li>experience == 0 (Req 12.2)
     *   <li>accumulatedLevel == before + 1 (Req 3.3)
     *   <li>abilityPoints == before + 1 (Req 1.4)
     *   <li>HP/MP/Stamina == vitalMaxFor(1, T) 각 필드 (Req 12.3)
     * </ul>
     *
     * @param level 현재 레벨 (2~100)
     * @param accExtra 누적레벨 추가분
     * @param currentTalent 환생 전 현재 재능
     * @param targetTalent 환생 시 선택할 재능
     * @param abilityPoints 환생 전 보유 AP
     */
    @Property(tries = 100)
    void should_applyTalentAndGrantAP_when_rebirthWithTalent(
            @ForAll("levels") final int level,
            @ForAll("accumulatedExtras") final int accExtra,
            @ForAll("talents") final TalentType currentTalent,
            @ForAll("talents") final TalentType targetTalent,
            @ForAll("abilityPointsArb") final int abilityPoints) {

        // Given: 환생 가능한 캐릭터 (lastRebirthAt=null → 첫 환생, 항상 가능)
        final int accumulatedLevel = level + accExtra;
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트캐릭터",
                        level,
                        accumulatedLevel,
                        0L,
                        currentTalent,
                        null,
                        50,
                        50,
                        50,
                        "tir-chonaill",
                        abilityPoints,
                        0L);

        final int beforeAccLevel = progress.getAccumulatedLevel();
        final int beforeAP = progress.getAbilityPoints();

        // When: 선택 재능으로 환생 실행
        final RebirthResult result = progressionService.rebirth(progress, targetTalent);

        // Then: 결과는 Reborn
        assertThat(result)
                .as("환생 가능 상태에서 결과는 Reborn이어야 한다")
                .isInstanceOf(RebirthResult.Reborn.class);

        // Then: talent == T (Req 12.1)
        assertThat(progress.getTalent()).as("환생 후 재능은 선택한 재능이어야 한다").isEqualTo(targetTalent);

        // Then: currentLevel == 1 (Req 12.2)
        assertThat(progress.getCurrentLevel()).as("환생 후 레벨은 1이어야 한다").isEqualTo(1);

        // Then: experience == 0 (Req 12.2)
        assertThat(progress.getExperience()).as("환생 후 경험치는 0이어야 한다").isEqualTo(0L);

        // Then: accumulatedLevel == before + 1 (Req 3.3)
        assertThat(progress.getAccumulatedLevel())
                .as("환생 후 누적레벨은 이전 대비 +1이어야 한다")
                .isEqualTo(beforeAccLevel + 1);

        // Then: abilityPoints == before + 1 (Req 1.4)
        assertThat(progress.getAbilityPoints())
                .as("환생 후 AP는 이전 대비 +1이어야 한다")
                .isEqualTo(beforeAP + 1);

        // Then: HP/MP/Stamina == vitalMaxFor(1, T) 각 필드 (Req 12.3)
        final VitalMax expectedVital = statProgression.vitalMaxFor(1, targetTalent);
        assertThat(progress.getHpCurrent())
                .as("환생 후 HP는 vitalMaxFor(1, T).hp()와 같아야 한다")
                .isEqualTo(expectedVital.hp());
        assertThat(progress.getMpCurrent())
                .as("환생 후 MP는 vitalMaxFor(1, T).mp()와 같아야 한다")
                .isEqualTo(expectedVital.mp());
        assertThat(progress.getStaminaCurrent())
                .as("환생 후 Stamina는 vitalMaxFor(1, T).stamina()와 같아야 한다")
                .isEqualTo(expectedVital.stamina());
    }

    /**
     * 레벨 생성기: 2~100 (환생 효과를 보여주기 위해 1보다 큰 값).
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
     * 재능 유형 생성기 (3종 모든 재능).
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 보유 AP 생성기: 0~200.
     *
     * @return AP Arbitrary
     */
    @Provide
    Arbitrary<Integer> abilityPointsArb() {
        return Arbitraries.integers().between(0, 200);
    }
}
