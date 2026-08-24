package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link SkillRankupBonus}의 합산 로직 단위 테스트. */
class SkillRankupBonusTest {

    private final SkillRankupBonus bonus = new SkillRankupBonus();

    @Test
    @DisplayName("windmill A(order 5, MELEE) → STR += 5")
    void should_addStrBonus_when_meleeSkillAtRankA() {
        final CharacterSkill windmill = new CharacterSkill(1L, "windmill", SkillRank.A, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                createLookup("windmill", SkillTalent.MELEE);

        final Stats result = bonus.sum(List.of(windmill), lookup);

        assertThat(result.str()).isEqualTo(5);
        assertThat(result.dex()).isZero();
        assertThat(result.intelligence()).isZero();
        assertThat(result.critical()).isZero();
        assertThat(result.defense()).isZero();
    }

    @Test
    @DisplayName("F 랭크(order 0) 스킬은 보너스 0")
    void should_addZeroBonus_when_rankIsF() {
        final CharacterSkill skill = new CharacterSkill(1L, "firebolt", SkillRank.F, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                createLookup("firebolt", SkillTalent.MAGIC);

        final Stats result = bonus.sum(List.of(skill), lookup);

        assertThat(result).isEqualTo(Stats.ZERO);
    }

    @Test
    @DisplayName(
            "7종 전부 MASTER(order 15): MELEE×2+ARCHERY×2+MAGIC×2+COMMON×1 → STR+30/DEX+30/INT+30/DEF+15")
    void should_sumAllBonuses_when_allSkillsAtMaster() {
        final List<CharacterSkill> owned =
                List.of(
                        new CharacterSkill(1L, "smash", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "windmill", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "magnum_shot", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "arrow_revolver", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "firebolt", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "icebolt", SkillRank.MASTER, 0, 0),
                        new CharacterSkill(1L, "defense", SkillRank.MASTER, 0, 0));

        final Function<String, Optional<Skill>> lookup =
                id -> {
                    final SkillTalent talent =
                            switch (id) {
                                case "smash", "windmill" -> SkillTalent.MELEE;
                                case "magnum_shot", "arrow_revolver" -> SkillTalent.ARCHERY;
                                case "firebolt", "icebolt" -> SkillTalent.MAGIC;
                                case "defense" -> SkillTalent.COMMON;
                                default -> null;
                            };
                    if (talent == null) {
                        return Optional.empty();
                    }
                    return Optional.of(createDamageSkill(id, talent));
                };

        final Stats result = bonus.sum(owned, lookup);

        assertThat(result.str()).isEqualTo(30);
        assertThat(result.dex()).isEqualTo(30);
        assertThat(result.intelligence()).isEqualTo(30);
        assertThat(result.defense()).isEqualTo(15);
        assertThat(result.critical()).isZero();

        final VitalMax vitalResult = bonus.sumVital(owned, lookup);
        assertThat(vitalResult.hp()).isEqualTo(75);
        assertThat(vitalResult.mp()).isZero();
        assertThat(vitalResult.stamina()).isZero();
    }

    @Test
    @DisplayName("defense 랭크업 시 DEF +1/rank, HP +5/rank 누적")
    void should_addDefAndHpBonus_when_defenseSkillRankedUp() {
        final CharacterSkill defense = new CharacterSkill(1L, "defense", SkillRank.R9, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                createLookup("defense", SkillTalent.COMMON);

        final Stats statResult = bonus.sum(List.of(defense), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(defense), lookup);

        // R9 order = 6
        assertThat(statResult.defense()).isEqualTo(6);
        assertThat(vitalResult.hp()).isEqualTo(30);
    }

    @Test
    @DisplayName("counter_attack은 랭크업해도 영구 스탯 및 HP 보너스가 0이다")
    void should_returnZeroBonus_when_counterAttackSkillRankedUp() {
        final CharacterSkill counter =
                new CharacterSkill(1L, "counter_attack", SkillRank.MASTER, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                createLookup("counter_attack", SkillTalent.COMMON);

        final Stats statResult = bonus.sum(List.of(counter), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(counter), lookup);

        assertThat(statResult).isEqualTo(Stats.ZERO);
        assertThat(vitalResult).isEqualTo(new VitalMax(0, 0, 0));
    }

    @Test
    @DisplayName("카탈로그에 없는 스킬은 보너스 계산에서 제외된다")
    void should_skipUnknownSkill_when_catalogLookupReturnsEmpty() {
        final CharacterSkill unknown = new CharacterSkill(1L, "unknown", SkillRank.MASTER, 0, 0);
        final Function<String, Optional<Skill>> lookup = id -> Optional.empty();

        final Stats result = bonus.sum(List.of(unknown), lookup);

        assertThat(result).isEqualTo(Stats.ZERO);
    }

    @Test
    @DisplayName("빈 목록이면 Stats.ZERO를 반환한다")
    void should_returnZero_when_noSkillsOwned() {
        final Function<String, Optional<Skill>> lookup = id -> Optional.empty();

        final Stats result = bonus.sum(List.of(), lookup);

        assertThat(result).isEqualTo(Stats.ZERO);
    }

    @Test
    @DisplayName("패시브 6종 랭크업 시 선형 스탯 및 바이탈 누적 가산")
    void should_addLinearPassiveBonus_when_passiveSkillsRankedUp() {
        // given
        // combat_mastery (STR +20, HP +50), R9 (order 6) -> STR: round(20*6/15)=8, HP:
        // round(50*6/15)=20
        final CharacterSkill combat = new CharacterSkill(1L, "combat_mastery", SkillRank.R9, 0, 0);
        // critical_hit (CRITICAL +100), MASTER (order 15) -> CRITICAL: 100
        final CharacterSkill crit = new CharacterSkill(1L, "critical_hit", SkillRank.MASTER, 0, 0);
        // meditation (MP +30), R1 (order 14) -> MP: round(30*14/15)=28
        final CharacterSkill medi = new CharacterSkill(1L, "meditation", SkillRank.R1, 0, 0);

        final PassiveSkill combatSkill =
                new PassiveSkill(
                        "combat_mastery",
                        "컴뱃 마스터리",
                        SkillType.PASSIVE,
                        SkillTalent.COMMON,
                        0,
                        Map.of(BonusTarget.STR, 20, BonusTarget.HP, 50),
                        "desc");
        final PassiveSkill critSkill =
                new PassiveSkill(
                        "critical_hit",
                        "크리티컬 히트",
                        SkillType.PASSIVE,
                        SkillTalent.COMMON,
                        0,
                        Map.of(BonusTarget.CRITICAL, 100),
                        "desc");
        final PassiveSkill mediSkill =
                new PassiveSkill(
                        "meditation",
                        "메디테이션",
                        SkillType.PASSIVE,
                        SkillTalent.COMMON,
                        0,
                        Map.of(BonusTarget.MP, 30),
                        "desc");

        final Function<String, Optional<Skill>> lookup =
                id ->
                        switch (id) {
                            case "combat_mastery" -> Optional.of(combatSkill);
                            case "critical_hit" -> Optional.of(critSkill);
                            case "meditation" -> Optional.of(mediSkill);
                            default -> Optional.empty();
                        };

        // when
        final Stats statResult = bonus.sum(List.of(combat, crit, medi), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(combat, crit, medi), lookup);
        final int regenResult = bonus.sumMpRegen(List.of(medi), lookup);

        // then
        assertThat(statResult.str()).isEqualTo(8);
        assertThat(statResult.critical()).isEqualTo(100);
        assertThat(vitalResult.hp()).isEqualTo(20);
        assertThat(vitalResult.mp()).isEqualTo(28);
        assertThat(regenResult).isEqualTo(5); // R1 order 14 -> 5 MP regen
    }

    @Test
    @DisplayName("메디테이션 미보유 시 sumMpRegen은 0을 반환")
    void should_returnZeroMpRegen_when_noMeditationOwned() {
        // given
        final Function<String, Optional<Skill>> lookup = id -> Optional.empty();

        // when
        final int regen = bonus.sumMpRegen(List.of(), lookup);

        // then
        assertThat(regen).isZero();
    }

    private Function<String, Optional<Skill>> createLookup(
            final String skillId, final SkillTalent talent) {
        final Skill skill = createDamageSkill(skillId, talent);
        return id -> id.equals(skillId) ? Optional.of(skill) : Optional.empty();
    }

    private DamageSkill createDamageSkill(final String id, final SkillTalent talent) {
        final Map<SkillRank, Integer> multiplierMap = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            multiplierMap.put(rank, 100 + rank.order() * 10);
        }
        return new DamageSkill(id, id, SkillType.NORMAL, talent, 10, multiplierMap, "test");
    }
}
