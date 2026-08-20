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
