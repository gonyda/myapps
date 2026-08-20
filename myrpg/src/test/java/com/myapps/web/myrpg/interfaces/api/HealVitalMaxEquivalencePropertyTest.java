package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.TopBarView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
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
 * 치료 후 활력치 상단바 최대치 완벽 일치 프로퍼티 테스트.
 *
 * <p>임의의 캐릭터 레벨·재능·장착 장비 보너스 상태에서 {@code POST /heal} 성공 후의 캐릭터 {@code hpCurrent}, {@code
 * mpCurrent}, {@code staminaCurrent}는 {@link PlayScreenViewHelper#buildTopBar}에서 산출하는 최대치({@code
 * vitalMax})와 정확히 일치함을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 9: 치료 후 활력치 상단바 최대치 완벽 일치
 *
 * <p><b>Validates: Requirements 11.3, 11.4</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 9: 치료 후 활력치 상단바 최대치 완벽 일치
class HealVitalMaxEquivalencePropertyTest {

    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;

    /**
     * 임의의 레벨·재능·장착 바이탈 보너스 상태에서 {@code POST /heal} 성공 후의 캐릭터 활력치가 {@link
     * PlayScreenViewHelper#buildTopBar} 최대치와 정확히 일치하는지 검증한다.
     *
     * @param level 임의 레벨 (1~100)
     * @param talent 임의 재능
     * @param vitalBonus 임의 장착 바이탈 보너스
     */
    @Property(tries = 100)
    void should_fullRecoverToTopBarVitalMax(
            @ForAll("level") final int level,
            @ForAll("talent") final TalentType talent,
            @ForAll("vitalBonus") final VitalMax vitalBonus) {
        final Fixture fixture = newFixture(level, talent, vitalBonus);

        fixture.healController().heal();

        final TopBarView topBar = fixture.viewHelper().buildTopBar(fixture.progress());
        assertThat(fixture.progress().getHpCurrent()).isEqualTo(topBar.hp().max());
        assertThat(fixture.progress().getMpCurrent()).isEqualTo(topBar.mp().max());
        assertThat(fixture.progress().getStaminaCurrent()).isEqualTo(topBar.stamina().max());
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 임의 레벨(1~100)을 생성한다.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> level() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 임의 재능을 생성한다.
     *
     * @return TalentType Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talent() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 임의 장착 바이탈 보너스(각 0~50)를 생성한다.
     *
     * @return VitalMax Arbitrary
     */
    @Provide
    Arbitrary<VitalMax> vitalBonus() {
        return Arbitraries.integers()
                .between(0, 50)
                .flatMap(
                        hp ->
                                Arbitraries.integers()
                                        .between(0, 50)
                                        .flatMap(
                                                mp ->
                                                        Arbitraries.integers()
                                                                .between(0, 50)
                                                                .map(
                                                                        stamina ->
                                                                                new VitalMax(
                                                                                        hp, mp,
                                                                                        stamina))));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * 테스트 픽스처: 치료 컨트롤러, 상단바 뷰 헬퍼, 캐릭터 진행상황.
     *
     * @param healController 치료 컨트롤러 (실제 구현)
     * @param viewHelper 상단바 뷰 헬퍼 (실제 구현)
     * @param progress 캐릭터 진행상황 (레벨/재능 설정됨)
     */
    record Fixture(
            HealController healController,
            PlayScreenViewHelper viewHelper,
            CharacterProgress progress) {}

    private Fixture newFixture(
            final int level, final TalentType talent, final VitalMax vitalBonus) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", level, level, 0L, talent, null, 50, 30, 20, "tir-chonaill", 0, 500L);

        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.equippedBonus())
                .thenReturn(new EquippedBonusResult(Stats.ZERO, vitalBonus));

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.loadOrCreateDefault()).thenReturn(progress);

        final HealController healController =
                new HealController(
                        characterService, statProgression, inventoryService, fixedAction());

        final PlayScreenViewHelper viewHelper =
                new PlayScreenViewHelper(
                        mock(ExperiencePolicy.class),
                        statProgression,
                        mock(SkillService.class),
                        inventoryService);

        return new Fixture(healController, viewHelper, progress);
    }

    private ActionLog fixedAction() {
        return new ActionLog(
                Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }
}
