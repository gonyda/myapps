package com.myapps.web.myrpg.interfaces.api;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import com.myapps.web.myrpg.application.dto.GaugeView;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 게이지 계산과 수치 오버레이 프로퍼티 테스트.
 *
 * <p>임의의 current/max 조합에 대해 퍼센트 계산 공식
 * {@code clamp(round(current*100/max), 0, 100)}과 오버레이 형식 {@code "current / max"}을 검증한다.
 * 또한 EXP 게이지의 max가 {@link ExperiencePolicy#requiredForNext(int)}로 결정됨을 확인한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 15: 게이지 계산과 수치 오버레이
 *
 * <p><b>Validates: Requirements 1.4, 1.5, 1.6, 1.11</b>
 */
class PlayScreenViewHelperGaugePropertyTest {

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final PlayScreenViewHelper helper = new PlayScreenViewHelper(
            experiencePolicy, statProgression, mock(SkillService.class));

    /**
     * 임의의 current(0~max)와 max(1~10000)에 대해 퍼센트가
     * {@code clamp(round(current*100/max), 0, 100)}이고,
     * 오버레이가 {@code "current / max"} 형식임을 검증한다.
     *
     * @param max     게이지 최대값 (1 이상)
     * @param current 게이지 현재값 (0 이상 max 이하)
     */
    @Property(tries = 100)
    void should_calculatePercentAndOverlay_when_currentWithinMax(
            @ForAll @IntRange(min = 1, max = 10000) final int max,
            @ForAll @IntRange(min = 0, max = 10000) final int current) {

        // current를 max 이하로 제한
        final int effectiveCurrent = Math.min(current, max);
        final GaugeView gauge = helper.buildGauge(effectiveCurrent, max);

        // 기대 퍼센트: clamp(round(current*100/max), 0, 100)
        final long rawPercent = Math.round((double) effectiveCurrent * 100 / max);
        final int expectedPercent = (int) Math.max(0, Math.min(100, rawPercent));

        assertThat(gauge.percent()).isEqualTo(expectedPercent);
        assertThat(gauge.overlay()).isEqualTo(effectiveCurrent + " / " + max);
    }

    /**
     * current=0일 때 항상 percent=0이고 오버레이가 {@code "0 / max"} 형식임을 검증한다.
     *
     * @param max 게이지 최대값 (1 이상)
     */
    @Property(tries = 100)
    void should_returnZeroPercentAndZeroOverlay_when_currentIsZero(
            @ForAll @IntRange(min = 1, max = 10000) final int max) {

        final GaugeView gauge = helper.buildGauge(0, max);

        assertThat(gauge.percent()).isEqualTo(0);
        assertThat(gauge.overlay()).isEqualTo("0 / " + max);
    }

    /**
     * EXP 게이지의 max가 {@link ExperiencePolicy#requiredForNext(int)}와 일치함을 검증한다.
     * 레벨 1~99 범위에서 buildTopBar가 올바른 EXP max를 사용하는지 확인한다.
     * (레벨 100은 최대레벨로서 EXP 게이지가 "MAX"로 표시되며 별도 프로퍼티 테스트에서 검증한다.)
     *
     * @param level 캐릭터 레벨 (1~99)
     */
    @Property(tries = 100)
    void should_useRequiredExpAsMax_when_buildingExpGauge(
            @ForAll @IntRange(min = 1, max = 99) final int level) {

        final CharacterProgress progress = new CharacterProgress(
                "고니",
                level,
                level,
                0L,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0
        );

        final TopBarView topBar = helper.buildTopBar(progress);
        final long expectedMax = experiencePolicy.requiredForNext(level);

        assertThat(topBar.exp().max()).isEqualTo((int) expectedMax);
    }
}
