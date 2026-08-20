package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * ProgressionService의 예시 기반 단위 테스트.
 *
 * <p>사망 패널티 구체 예시, 경험치 곡선 샘플값, 신규 캐릭터 기본값을 검증한다.
 */
class ProgressionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final ProgressionService service =
            new ProgressionService(experiencePolicy, statProgression, FIXED_CLOCK);

    /**
     * 사망 패널티: 레벨 1, 경험치 23/100 → 13/100. penalty = floor(100 × 0.10) = 10, newExp = max(0, 23 - 10)
     * = 13, experienceLost = 10.
     */
    @Test
    void should_reduceTo13_when_deathPenaltyAt23ExpLevel1() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        23L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        final DeathResult result = service.applyDeathPenalty(progress);

        assertThat(progress.getExperience()).isEqualTo(13L);
        assertThat(result.experienceLost()).isEqualTo(10L);
    }

    /**
     * 사망 패널티: 레벨 1, 경험치 5/100 → 0/100. penalty = floor(100 × 0.10) = 10, newExp = max(0, 5 - 10) =
     * 0, experienceLost = 5 (실제 차감량).
     */
    @Test
    void should_reduceTo0_when_deathPenaltyAt5ExpLevel1() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        5L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        final DeathResult result = service.applyDeathPenalty(progress);

        assertThat(progress.getExperience()).isEqualTo(0L);
        assertThat(result.experienceLost()).isEqualTo(5L);
    }

    /** 경험치 곡선 샘플: L1→100, L2→400, L10→10000. */
    @Test
    void should_returnCorrectCurveValues_when_queryingExperiencePolicy() {
        assertThat(experiencePolicy.requiredForNext(1)).isEqualTo(100L);
        assertThat(experiencePolicy.requiredForNext(2)).isEqualTo(400L);
        assertThat(experiencePolicy.requiredForNext(10)).isEqualTo(10000L);
    }

    /** 신규 캐릭터 기본값: 재능 MELEE, 바이탈 100, Critical 50 (0.1%단위 = 5.0%). */
    @Test
    void should_haveCorrectDefaults_when_createDefaultCharacter() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        assertThat(progress.getTalent()).isEqualTo(TalentType.MELEE);
        assertThat(progress.getHpCurrent()).isEqualTo(100);
        assertThat(progress.getMpCurrent()).isEqualTo(100);
        assertThat(progress.getStaminaCurrent()).isEqualTo(100);

        final Stats levelOneStats = statProgression.levelStatsFor(1);
        assertThat(levelOneStats.critical()).isEqualTo(50);
    }
}
