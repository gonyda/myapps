package com.myapps.web.myrpg.interfaces.api;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.application.dto.InfoPopupView;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.application.dto.StatLine;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PlayScreenViewHelper#buildInfo(CharacterProgress, RebirthStatus)} 단위 테스트.
 *
 * <p>StatLine 형식, 재능 라벨, 환생 경과/기록 없음 텍스트, AP 매핑,
 * 재능 효과 요약 매핑, 재능 반영 중앙 스탯, 바이탈별 게이지 max를 검증한다.
 *
 * <p><b>Validates: Requirements 4.1, 4.3, 8.4, 8.5, 10.2, 10.3, 10.7</b>
 */
class PlayScreenViewHelperInfoTest {

    private PlayScreenViewHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PlayScreenViewHelper(new ExperiencePolicy(), new StatProgression());
    }

    @Test
    void should_buildStatLines_when_defaultLevel1Character() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        final List<StatLine> stats = info.stats();
        assertThat(stats).hasSize(5);

        // Lv1: STR=10, DEX=10, INT=10, CRIT=50(=5.0%), DEF=5
        assertThat(stats.get(0)).isEqualTo(new StatLine("STR", "10", "+0"));
        assertThat(stats.get(1)).isEqualTo(new StatLine("DEX", "10", "+0"));
        assertThat(stats.get(2)).isEqualTo(new StatLine("INT", "10", "+0"));
        assertThat(stats.get(3)).isEqualTo(new StatLine("CRIT", "5.0%", "+0.0%"));
        assertThat(stats.get(4)).isEqualTo(new StatLine("DEF", "5", "+0"));
    }

    @Test
    void should_buildStatLines_when_higherLevelCharacter() {
        // Lv10 MELEE: STR=10+3*9+2*9=55, DEX=10+3*9=37, INT=37, CRIT=50+3*9=77(=7.7%), DEF=5+1*9=14
        final CharacterProgress progress = new CharacterProgress(
                "고니",
                10,
                10,
                0L,
                TalentType.MELEE,
                null,
                190,
                190,
                190,
                "tir-chonaill",
                0
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        final List<StatLine> stats = info.stats();
        assertThat(stats.get(0)).isEqualTo(new StatLine("STR", "55", "+0"));
        assertThat(stats.get(1)).isEqualTo(new StatLine("DEX", "37", "+0"));
        assertThat(stats.get(2)).isEqualTo(new StatLine("INT", "37", "+0"));
        assertThat(stats.get(3)).isEqualTo(new StatLine("CRIT", "7.7%", "+0.0%"));
        assertThat(stats.get(4)).isEqualTo(new StatLine("DEF", "14", "+0"));
    }

    @Test
    void should_showTalentLabel_when_defaultCharacter() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.talentLabel()).isEqualTo("근접전투");
    }

    @Test
    void should_showTalentLabel_when_archeryTalent() {
        final CharacterProgress progress = new CharacterProgress(
                "궁수",
                5,
                5,
                0L,
                TalentType.ARCHERY,
                null,
                140,
                140,
                140,
                "tir-chonaill",
                0
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.talentLabel()).isEqualTo("활");
    }

    @Test
    void should_showRebirthElapsedText_when_neverRebirthed() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.rebirthElapsedText()).isEqualTo("환생 기록 없음");
    }

    @Test
    void should_showRebirthElapsedText_when_rebirthed3Hours15MinutesAgo() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Duration elapsed = Duration.ofHours(3).plusMinutes(15);
        final Duration remaining = Duration.ofHours(20).plusMinutes(45);
        final RebirthStatus status = new RebirthStatus(false, true, elapsed, remaining);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.rebirthElapsedText()).isEqualTo("환생 후 3시간 15분 경과");
        assertThat(info.rebirthAvailable()).isFalse();
    }

    @Test
    void should_showRebirthElapsedText_when_rebirthed25HoursAgo() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        final Duration elapsed = Duration.ofHours(25).plusMinutes(30);
        final RebirthStatus status = new RebirthStatus(true, true, elapsed, Duration.ZERO);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.rebirthElapsedText()).isEqualTo("환생 후 25시간 30분 경과");
        assertThat(info.rebirthAvailable()).isTrue();
    }

    @Test
    void should_showNicknameAndLevels_when_buildInfo() {
        final CharacterProgress progress = new CharacterProgress(
                "전사",
                15,
                30,
                500L,
                TalentType.MAGIC,
                null,
                240,
                240,
                240,
                "dunbarton",
                0
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.nickname()).isEqualTo("전사");
        assertThat(info.currentLevel()).isEqualTo(15);
        assertThat(info.accumulatedLevel()).isEqualTo(30);
        assertThat(info.talentLabel()).isEqualTo("마법");
    }

    @Test
    void should_buildVitalGauges_when_buildInfo() {
        // Lv5 MELEE: HP max = 100 + 10*4 + 5*4 = 160, MP/Stamina max = 100 + 10*4 = 140
        final CharacterProgress progress = new CharacterProgress(
                "고니",
                5,
                5,
                0L,
                TalentType.MELEE,
                null,
                70,
                100,
                140,
                "tir-chonaill",
                0
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.hp().current()).isEqualTo(70);
        assertThat(info.hp().max()).isEqualTo(160);
        assertThat(info.hp().percent()).isEqualTo(44);
        assertThat(info.mp().current()).isEqualTo(100);
        assertThat(info.mp().max()).isEqualTo(140);
        assertThat(info.stamina().current()).isEqualTo(140);
        assertThat(info.stamina().max()).isEqualTo(140);
        assertThat(info.stamina().percent()).isEqualTo(100);
    }

    /**
     * buildInfo 결과의 abilityPoints가 progress.getAbilityPoints()와 일치하는지 검증한다.
     *
     * <p><b>Validates: Requirements 4.1, 4.3</b>
     */
    @Test
    void should_mapAbilityPoints_when_buildInfo() {
        final CharacterProgress progress = new CharacterProgress(
                "전사",
                10,
                15,
                0L,
                TalentType.MELEE,
                null,
                190,
                190,
                190,
                "tir-chonaill",
                14
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.abilityPoints()).isEqualTo(progress.getAbilityPoints());
        assertThat(info.abilityPoints()).isEqualTo(14);
    }

    /**
     * buildInfo 결과의 talentEffectSummary가 progress.getTalent().effectSummary()와 일치하는지 검증한다.
     *
     * <p><b>Validates: Requirements 10.2</b>
     */
    @Test
    void should_mapTalentEffectSummary_when_buildInfo() {
        final CharacterProgress progress = new CharacterProgress(
                "마법사",
                5,
                5,
                0L,
                TalentType.MAGIC,
                null,
                140,
                140,
                140,
                "dunbarton",
                4
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.talentEffectSummary()).isEqualTo(progress.getTalent().effectSummary());
        assertThat(info.talentEffectSummary()).isEqualTo("마법 데미지 +10%, INT +2/Lv, MP +5/Lv");
    }

    /**
     * ARCHERY Lv.10 재능 반영 중앙 스탯 본체를 검증한다.
     *
     * <p>ARCHERY Lv.10: STR=37, DEX=55, INT=37, CRIT=86(="8.6%"), DEF=14.
     * 주 스탯(DEX)과 보조(CRITICAL)에 재능 보너스가 반영된다.
     *
     * <p><b>Validates: Requirements 4.1, 8.4</b>
     */
    @Test
    void should_reflectTalentInStats_when_archeryLevel10() {
        // ARCHERY Lv10: DEX=10+3*9+2*9=55, CRIT=50+3*9+1*9=86(=8.6%), STR/INT=37, DEF=14
        final CharacterProgress progress = new CharacterProgress(
                "궁수",
                10,
                10,
                0L,
                TalentType.ARCHERY,
                null,
                190,
                190,
                190,
                "tir-chonaill",
                9
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        final List<StatLine> stats = info.stats();
        assertThat(stats.get(0)).isEqualTo(new StatLine("STR", "37", "+0"));
        assertThat(stats.get(1)).isEqualTo(new StatLine("DEX", "55", "+0"));
        assertThat(stats.get(2)).isEqualTo(new StatLine("INT", "37", "+0"));
        assertThat(stats.get(3)).isEqualTo(new StatLine("CRIT", "8.6%", "+0.0%"));
        assertThat(stats.get(4)).isEqualTo(new StatLine("DEF", "14", "+0"));
    }

    /**
     * HP/MP/Stamina 게이지 max가 vitalMaxFor(level, talent) 각 필드와 일치하는지 검증한다.
     *
     * <p>MAGIC Lv.10: HP/Stamina max=190(공통), MP max=235(공통+보조 MP +5*9).
     *
     * <p><b>Validates: Requirements 8.4, 8.5</b>
     */
    @Test
    void should_matchVitalMaxForEachField_when_magicTalentLevel10() {
        // MAGIC Lv10: HP max = 100+10*9 = 190, MP max = 100+10*9+5*9 = 235, Stamina max = 190
        final StatProgression statProgression = new StatProgression();
        final VitalMax expectedMax = statProgression.vitalMaxFor(10, TalentType.MAGIC);

        final CharacterProgress progress = new CharacterProgress(
                "마법사",
                10,
                10,
                0L,
                TalentType.MAGIC,
                null,
                180,
                200,
                150,
                "dunbarton",
                9
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.hp().max()).isEqualTo(expectedMax.hp());
        assertThat(info.hp().max()).isEqualTo(190);
        assertThat(info.mp().max()).isEqualTo(expectedMax.mp());
        assertThat(info.mp().max()).isEqualTo(235);
        assertThat(info.stamina().max()).isEqualTo(expectedMax.stamina());
        assertThat(info.stamina().max()).isEqualTo(190);
    }

    /**
     * ARCHERY Lv.10의 바이탈 게이지 max가 세 바이탈 모두 공통값(190)인지 검증한다.
     *
     * <p>ARCHERY는 보조 보너스가 CRITICAL(스탯 계열)이므로 바이탈에 보너스가 없다.
     *
     * <p><b>Validates: Requirements 8.4, 8.5</b>
     */
    @Test
    void should_matchVitalMaxForEachField_when_archeryTalentLevel10() {
        final StatProgression statProgression = new StatProgression();
        final VitalMax expectedMax = statProgression.vitalMaxFor(10, TalentType.ARCHERY);

        final CharacterProgress progress = new CharacterProgress(
                "궁수",
                10,
                10,
                0L,
                TalentType.ARCHERY,
                null,
                190,
                190,
                190,
                "tir-chonaill",
                9
        );
        final RebirthStatus status = new RebirthStatus(true, false, null, null);

        final InfoPopupView info = helper.buildInfo(progress, status);

        assertThat(info.hp().max()).isEqualTo(expectedMax.hp());
        assertThat(info.hp().max()).isEqualTo(190);
        assertThat(info.mp().max()).isEqualTo(expectedMax.mp());
        assertThat(info.mp().max()).isEqualTo(190);
        assertThat(info.stamina().max()).isEqualTo(expectedMax.stamina());
        assertThat(info.stamina().max()).isEqualTo(190);
    }
}
