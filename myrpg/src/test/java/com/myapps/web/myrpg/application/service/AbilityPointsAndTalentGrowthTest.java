package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * AP(어빌리티 포인트)와 재능별 성장 계산의 예시 기반 단위 테스트.
 *
 * <p>신규 생성 AP 0, 레벨업 3회 시 AP 3, 환생 후 AP+1·재능 반영을 검증하고, 재능별 스탯·바이탈 계산 샘플(MAGIC Lv.10→INT 55, ARCHERY
 * Lv.10→Critical 86, MELEE Lv.10→HP 235/MP·Stamina 190, ARCHERY Lv.10→세 바이탈 190)을 확인한다.
 */
class AbilityPointsAndTalentGrowthTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final ProgressionService service =
            new ProgressionService(experiencePolicy, statProgression, FIXED_CLOCK);

    /** 신규 캐릭터 생성 직후 AP가 0임을 검증한다. */
    @Test
    void should_haveZeroAP_when_newCharacterCreated() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        assertThat(progress.getAbilityPoints()).isEqualTo(0);
    }

    /**
     * 레벨업 3회 시 AP가 3 증가함을 검증한다.
     *
     * <p>레벨 1→2 필요 경험치 65, 레벨 2→3 필요 160, 레벨 3→4 필요 285. 각 레벨업을 유발할 만큼 경험치를 획득한 뒤 AP를 확인한다.
     */
    @Test
    void should_haveThreeAP_when_levelUpThreeTimes() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        // Lv1→2: requiredForNext(1) = 65
        service.gainExperience(progress, experiencePolicy.requiredForNext(1));
        // Lv2→3: requiredForNext(2) = 160
        service.gainExperience(progress, experiencePolicy.requiredForNext(2));
        // Lv3→4: requiredForNext(3) = 285
        service.gainExperience(progress, experiencePolicy.requiredForNext(3));

        assertThat(progress.getCurrentLevel()).isEqualTo(4);
        assertThat(progress.getAbilityPoints()).isEqualTo(3);
    }

    /**
     * 환생 시 AP가 +1 증가하고 선택 재능이 반영됨을 검증한다.
     *
     * <p>레벨업 2회(AP=2) 후 ARCHERY 재능으로 환생하면 AP=3, 재능=ARCHERY.
     */
    @Test
    void should_grantOneAPAndApplyTalent_when_rebirth() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        // 레벨업 2회
        service.gainExperience(progress, experiencePolicy.requiredForNext(1));
        service.gainExperience(progress, experiencePolicy.requiredForNext(2));
        assertThat(progress.getAbilityPoints()).isEqualTo(2);

        final int apBefore = progress.getAbilityPoints();
        service.rebirth(progress, TalentType.ARCHERY);

        assertThat(progress.getAbilityPoints()).isEqualTo(apBefore + 1);
        assertThat(progress.getTalent()).isEqualTo(TalentType.ARCHERY);
    }

    /**
     * MAGIC 재능 Lv.10에서 INT = 55임을 검증한다.
     *
     * <p>공식: 10 + 3×9 + 2×9 = 10 + 27 + 18 = 55.
     */
    @Test
    void should_calculateMagicIntAt55_when_level10() {
        final Stats stats = statProgression.levelStatsFor(10, TalentType.MAGIC);

        assertThat(stats.intelligence()).isEqualTo(55);
    }

    /**
     * ARCHERY 재능 Lv.10에서 Critical = 86 (8.6%)임을 검증한다.
     *
     * <p>공식: 50 + 3×9 + 1×9 = 50 + 27 + 9 = 86.
     */
    @Test
    void should_calculateArcheryCriticalAt86_when_level10() {
        final Stats stats = statProgression.levelStatsFor(10, TalentType.ARCHERY);

        assertThat(stats.critical()).isEqualTo(86);
    }

    /**
     * MELEE 재능 Lv.10에서 HP 최대치 = 235, MP/Stamina = 190임을 검증한다.
     *
     * <p>HP 공식: 100 + 10×9 + 5×9 = 100 + 90 + 45 = 235.
     *
     * <p>MP/Stamina 공식: 100 + 10×9 = 190 (바이탈 보너스 없음).
     */
    @Test
    void should_calculateMeleeHPAt235_when_level10() {
        final VitalMax vitalMax = statProgression.vitalMaxFor(10, TalentType.MELEE);

        assertThat(vitalMax.hp()).isEqualTo(235);
        assertThat(vitalMax.mp()).isEqualTo(190);
        assertThat(vitalMax.stamina()).isEqualTo(190);
    }

    /**
     * ARCHERY 재능 Lv.10에서 세 바이탈(HP/MP/Stamina) 모두 190임을 검증한다.
     *
     * <p>ARCHERY의 보조 보너스는 CRITICAL(스탯 계열)이므로 바이탈에 가산되지 않는다. 세 바이탈 공통: 100 + 10×9 = 190.
     */
    @Test
    void should_calculateArcheryAllVitalsAt190_when_level10() {
        final VitalMax vitalMax = statProgression.vitalMaxFor(10, TalentType.ARCHERY);

        assertThat(vitalMax.hp()).isEqualTo(190);
        assertThat(vitalMax.mp()).isEqualTo(190);
        assertThat(vitalMax.stamina()).isEqualTo(190);
    }
}
