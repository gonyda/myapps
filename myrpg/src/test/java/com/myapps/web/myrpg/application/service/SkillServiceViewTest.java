package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRankUpView;
import com.myapps.web.myrpg.application.dto.SkillRowView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SkillService 뷰 조립 메서드의 단위 테스트.
 *
 * <p>buildListView와 buildRankUpView의 핵심 로직(탭 필터·진행바·rankable·모달 수치)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SkillServiceViewTest {

    private static final Long CHARACTER_ID = 1L;
    private static final String WINDMILL_ID = "windmill";
    private static final String DEFENSE_ID = "defense";
    private static final String COUNTER_ATTACK_ID = "counter_attack";

    @Mock private CharacterSkillRepository characterSkillRepository;

    @Mock private CharacterProgressRepository characterProgressRepository;

    @Mock private SkillCatalogService skillCatalogService;

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        skillService =
                new SkillService(
                        characterSkillRepository, characterProgressRepository, skillCatalogService);
    }

    @Test
    void should_buildListView_with_all_tab_returning_all_skills() {
        final CharacterSkill windmillSkill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 3, 0);
        final CharacterSkill defenseSkill =
                new CharacterSkill(CHARACTER_ID, DEFENSE_ID, SkillRank.A, 50, 15);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID))
                .thenReturn(List.of(windmillSkill, defenseSkill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));
        when(skillCatalogService.byId(DEFENSE_ID)).thenReturn(Optional.of(createDefense()));

        final SkillListView view = skillService.buildListView(CHARACTER_ID, "all");

        assertThat(view.activeTab()).isEqualTo("all");
        assertThat(view.rows()).hasSize(2);

        final SkillRowView windmillRow = view.rows().get(0);
        assertThat(windmillRow.id()).isEqualTo(WINDMILL_ID);
        assertThat(windmillRow.label()).isEqualTo("윈드밀");
        assertThat(windmillRow.talentLabel()).isEqualTo("근접전투");
        assertThat(windmillRow.rankLabel()).isEqualTo("F");
        assertThat(windmillRow.maxed()).isFalse();
        // usage 3/5 = 0.6, kill 0/1 = 0 → (0.6+0)/2*100 = 30
        assertThat(windmillRow.progressPercent()).isEqualTo(30);
        assertThat(windmillRow.rankable()).isFalse();
    }

    @Test
    void should_buildListView_filtering_by_melee_tab() {
        final CharacterSkill windmillSkill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 5, 1);
        final CharacterSkill defenseSkill =
                new CharacterSkill(CHARACTER_ID, DEFENSE_ID, SkillRank.F, 0, 0);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID))
                .thenReturn(List.of(windmillSkill, defenseSkill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));
        when(skillCatalogService.byId(DEFENSE_ID)).thenReturn(Optional.of(createDefense()));

        final SkillListView view = skillService.buildListView(CHARACTER_ID, "melee");

        assertThat(view.activeTab()).isEqualTo("melee");
        assertThat(view.rows()).hasSize(1);
        assertThat(view.rows().get(0).id()).isEqualTo(WINDMILL_ID);
    }

    @Test
    void should_buildListView_with_rankable_when_conditions_and_ap_met() {
        // F→E: usage 5, kill 1, AP 1
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 5, 1);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID)).thenReturn(List.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));

        final SkillListView view = skillService.buildListView(CHARACTER_ID, "all");

        final SkillRowView row = view.rows().get(0);
        assertThat(row.rankable()).isTrue();
        assertThat(row.progressPercent()).isEqualTo(100);
    }

    @Test
    void should_buildListView_with_master_skill_showing_100_percent_and_maxed() {
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.MASTER, 0, 0);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID)).thenReturn(List.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));

        final SkillListView view = skillService.buildListView(CHARACTER_ID, "all");

        final SkillRowView row = view.rows().get(0);
        assertThat(row.maxed()).isTrue();
        assertThat(row.rankable()).isFalse();
        assertThat(row.progressPercent()).isEqualTo(100);
    }

    @Test
    void should_buildRankUpView_for_damage_skill_at_rank_F() {
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 3, 0);
        final CharacterProgress progress = createProgressWithAp(10);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));

        final SkillRankUpView view = skillService.buildRankUpView(CHARACTER_ID, WINDMILL_ID);

        assertThat(view.id()).isEqualTo(WINDMILL_ID);
        assertThat(view.label()).isEqualTo("윈드밀");
        assertThat(view.currentRankLabel()).isEqualTo("F");
        assertThat(view.nextRankLabel()).isEqualTo("E");
        assertThat(view.primaryStatLabel()).isEqualTo("보너스 데미지");
        assertThat(view.currentCounterValue()).isNull();
        assertThat(view.nextCounterValue()).isNull();
        assertThat(view.resourceKindLabel()).isEqualTo("스태미나");
        assertThat(view.resourceCost()).isEqualTo(15);
        assertThat(view.nextResourceCost()).isEqualTo(15);
        assertThat(view.currentCritBonus()).isNull();
        assertThat(view.nextCritBonus()).isNull();
        assertThat(view.rankupBonusText()).isEqualTo("체력(STR) +1");
        assertThat(view.usageCurrent()).isEqualTo(3);
        assertThat(view.usageRequired()).isEqualTo(5);
        assertThat(view.killCurrent()).isZero();
        assertThat(view.killRequired()).isEqualTo(1);
        assertThat(view.apCost()).isEqualTo(1);
        assertThat(view.apOwned()).isEqualTo(10);
        assertThat(view.rankable()).isFalse();
        assertThat(view.maxed()).isFalse();
    }

    @Test
    void should_buildRankUpView_for_defense_skill_without_counter_value_and_with_hp_def_bonus() {
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, DEFENSE_ID, SkillRank.F, 5, 0);
        final CharacterProgress progress = createProgressWithAp(5);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, DEFENSE_ID))
                .thenReturn(Optional.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(DEFENSE_ID)).thenReturn(Optional.of(createDefense()));

        final SkillRankUpView view = skillService.buildRankUpView(CHARACTER_ID, DEFENSE_ID);

        assertThat(view.id()).isEqualTo(DEFENSE_ID);
        assertThat(view.primaryStatLabel()).isEqualTo("피해 경감");
        assertThat(view.currentValue()).isEqualTo(100);
        assertThat(view.nextValue()).isEqualTo(100);
        assertThat(view.currentCounterValue()).isNull();
        assertThat(view.nextCounterValue()).isNull();
        assertThat(view.resourceKindLabel()).isEqualTo("스태미나");
        assertThat(view.resourceCost()).isEqualTo(5);
        assertThat(view.nextResourceCost()).isEqualTo(5);
        assertThat(view.currentCritBonus()).isNull();
        assertThat(view.nextCritBonus()).isNull();
        assertThat(view.rankupBonusText()).isEqualTo("방어력(DEF) +1, 최대 HP +5");
        assertThat(view.hasKillRequirement()).isFalse();
        assertThat(view.hasUsageRequirement()).isTrue();
        assertThat(view.rankable()).isTrue();
        assertThat(view.maxed()).isFalse();
    }

    @Test
    void should_buildRankUpView_for_counter_attack_skill_with_counter_and_crit_values() {
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, COUNTER_ATTACK_ID, SkillRank.F, 5, 0);
        final CharacterProgress progress = createProgressWithAp(5);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, COUNTER_ATTACK_ID))
                .thenReturn(Optional.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(COUNTER_ATTACK_ID))
                .thenReturn(Optional.of(createCounterAttack()));

        final SkillRankUpView view = skillService.buildRankUpView(CHARACTER_ID, COUNTER_ATTACK_ID);

        assertThat(view.id()).isEqualTo(COUNTER_ATTACK_ID);
        assertThat(view.primaryStatLabel()).isEqualTo("피해 경감");
        assertThat(view.currentValue()).isEqualTo(100);
        assertThat(view.nextValue()).isEqualTo(100);
        assertThat(view.currentCounterValue()).isEqualTo(100);
        assertThat(view.nextCounterValue()).isEqualTo(105);
        assertThat(view.currentCritBonus()).isZero();
        assertThat(view.nextCritBonus()).isEqualTo(10);
        assertThat(view.resourceCost()).isEqualTo(8);
        assertThat(view.nextResourceCost()).isEqualTo(8);
        assertThat(view.rankupBonusText()).isNull();
        assertThat(view.rankable()).isTrue();
        assertThat(view.maxed()).isFalse();
    }

    @Test
    void should_buildRankUpView_for_master_skill_with_null_nextRankLabel() {
        final CharacterSkill skill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.MASTER, 0, 0);
        final CharacterProgress progress = createProgressWithAp(100);

        when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, WINDMILL_ID))
                .thenReturn(Optional.of(skill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));

        final SkillRankUpView view = skillService.buildRankUpView(CHARACTER_ID, WINDMILL_ID);

        assertThat(view.currentRankLabel()).isEqualTo("Master");
        assertThat(view.nextRankLabel()).isNull();
        assertThat(view.maxed()).isTrue();
        assertThat(view.rankable()).isFalse();
        assertThat(view.nextResourceCost()).isNull();
        assertThat(view.nextCritBonus()).isNull();
        assertThat(view.usageRequired()).isZero();
        assertThat(view.killRequired()).isZero();
        assertThat(view.apCost()).isZero();
    }

    @Test
    void should_buildListView_filtering_by_common_tab_showing_defense_only() {
        final CharacterSkill windmillSkill =
                new CharacterSkill(CHARACTER_ID, WINDMILL_ID, SkillRank.F, 0, 0);
        final CharacterSkill defenseSkill =
                new CharacterSkill(CHARACTER_ID, DEFENSE_ID, SkillRank.F, 0, 0);
        final CharacterProgress progress = createProgressWithAp(0);

        when(characterSkillRepository.findByCharacterId(CHARACTER_ID))
                .thenReturn(List.of(windmillSkill, defenseSkill));
        when(characterProgressRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(progress));
        when(skillCatalogService.byId(WINDMILL_ID)).thenReturn(Optional.of(createWindmill()));
        when(skillCatalogService.byId(DEFENSE_ID)).thenReturn(Optional.of(createDefense()));

        final SkillListView view = skillService.buildListView(CHARACTER_ID, "common");

        assertThat(view.rows()).hasSize(1);
        assertThat(view.rows().get(0).id()).isEqualTo(DEFENSE_ID);
        assertThat(view.rows().get(0).talentLabel()).isEqualTo("공용");
    }

    private CharacterProgress createProgressWithAp(final int ap) {
        return new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "tir-chonaill", ap, 0L);
    }

    private DamageSkill createWindmill() {
        final Map<SkillRank, Integer> multiplierByRank = new EnumMap<>(SkillRank.class);
        int base = 100;
        for (final SkillRank rank : SkillRank.values()) {
            multiplierByRank.put(rank, base);
            base += 10;
        }
        return new DamageSkill(
                WINDMILL_ID,
                "윈드밀",
                SkillType.NORMAL,
                SkillTalent.MELEE,
                15,
                Map.copyOf(multiplierByRank),
                "전방위 공격");
    }

    private DefenseSkill createDefense() {
        final Map<SkillRank, Integer> blockRateByRank = new EnumMap<>(SkillRank.class);
        final Map<SkillRank, Integer> counterMultiplierByRank = new EnumMap<>(SkillRank.class);
        final Map<SkillRank, Integer> resourceCostByRank = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            blockRateByRank.put(rank, 100);
            counterMultiplierByRank.put(rank, 0);
            resourceCostByRank.put(rank, 5);
        }
        return new DefenseSkill(
                DEFENSE_ID,
                "디펜스",
                SkillType.DEFENSE,
                SkillTalent.COMMON,
                5,
                Map.copyOf(blockRateByRank),
                Map.copyOf(counterMultiplierByRank),
                "완전 방어",
                Map.copyOf(resourceCostByRank),
                Map.of());
    }

    private DefenseSkill createCounterAttack() {
        final Map<SkillRank, Integer> blockRateByRank = new EnumMap<>(SkillRank.class);
        final Map<SkillRank, Integer> counterMultiplierByRank = new EnumMap<>(SkillRank.class);
        final Map<SkillRank, Integer> critBonusByRank = new EnumMap<>(SkillRank.class);
        int counter = 100;
        int crit = 0;
        for (final SkillRank rank : SkillRank.values()) {
            blockRateByRank.put(rank, 100);
            counterMultiplierByRank.put(rank, counter);
            critBonusByRank.put(rank, crit);
            counter += 5;
            crit += 10;
        }
        return new DefenseSkill(
                COUNTER_ATTACK_ID,
                "카운터 어택",
                SkillType.DEFENSE,
                SkillTalent.COMMON,
                8,
                Map.copyOf(blockRateByRank),
                Map.copyOf(counterMultiplierByRank),
                "반격 공격",
                Map.of(),
                Map.copyOf(critBonusByRank));
    }
}
