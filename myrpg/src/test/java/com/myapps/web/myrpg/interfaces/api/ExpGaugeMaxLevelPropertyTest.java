package com.myapps.web.myrpg.interfaces.api;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EXP 게이지와 최대레벨 표기 프로퍼티 테스트.
 *
 * <p>레벨이 100 미만이면 percent와 overlay가 경험치/필요치로 계산되고,
 * 레벨이 100이면 percent=100, overlay="MAX"임을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 12: EXP 게이지와 최대레벨 표기
 *
 * <p><b>Validates: Requirements 2.5, 2.6</b>
 */
class ExpGaugeMaxLevelPropertyTest {

    private static final int MAX_LEVEL = 100;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final StatProgression statProgression = new StatProgression();
    private final PlayScreenViewHelper helper;

    ExpGaugeMaxLevelPropertyTest() {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus()).thenReturn(EquippedBonusResult.ZERO);
        helper = new PlayScreenViewHelper(
                experiencePolicy, statProgression, mock(SkillService.class), inventoryService);
    }

    /**
     * 레벨이 100 미만일 때 EXP 게이지의 percent와 overlay가
     * 경험치/필요치 기반으로 정확히 계산됨을 검증한다.
     *
     * @param level      캐릭터 레벨 (1~99)
     * @param experience 현재 경험치 (0 이상)
     */
    @Property(tries = 100)
    void should_calculateExpGauge_when_levelBelowMax(
            @ForAll @IntRange(min = 1, max = 99) final int level,
            @ForAll @LongRange(min = 0, max = 999999) final long experience) {

        final long requiredExp = experiencePolicy.requiredForNext(level);
        final long effectiveExp = Math.min(experience, requiredExp - 1);

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                level,
                level,
                effectiveExp,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0, 0L
        );

        final TopBarView topBar = helper.buildTopBar(progress);

        final int expectedPercent = (int) Math.max(0,
                Math.min(100, Math.round((double) effectiveExp * 100 / requiredExp)));
        final String expectedOverlay = (int) effectiveExp + " / " + (int) requiredExp;

        assertThat(topBar.exp().percent()).isEqualTo(expectedPercent);
        assertThat(topBar.exp().overlay()).isEqualTo(expectedOverlay);
    }

    /**
     * 레벨이 100일 때 EXP 게이지의 percent=100, overlay="MAX"임을 검증한다.
     *
     * @param accumulatedLevel 누적 레벨 (100 이상)
     */
    @Property(tries = 100)
    void should_showMaxOverlay_when_levelIsMax(
            @ForAll @IntRange(min = 100, max = 500) final int accumulatedLevel) {

        final CharacterProgress progress = new CharacterProgress(
                "테스트",
                MAX_LEVEL,
                accumulatedLevel,
                0L,
                TalentType.MELEE,
                null,
                100,
                100,
                100,
                "tir-chonaill",
                0, 0L
        );

        final TopBarView topBar = helper.buildTopBar(progress);

        assertThat(topBar.exp().percent()).isEqualTo(100);
        assertThat(topBar.exp().overlay()).isEqualTo("MAX");
    }
}
