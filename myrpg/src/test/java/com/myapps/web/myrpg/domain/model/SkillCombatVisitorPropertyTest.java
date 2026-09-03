package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.service.SkillCombatVisitor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 020 스킬 타입 다형화 Property 1(동치성) 및 Property 2(디스패치 정합성) jqwik 검증.
 *
 * <p>8종 서브타입의 임의 인스턴스와 임의 SkillRank에 대해:
 *
 * <ul>
 *   <li>Property 1: 다형 값 메서드가 기존 서비스 분기 로직과 100% 동일한 값을 산출함.
 *   <li>Property 2: skill.accept(visitor)가 해당 서브타입 전용 visit 메서드를 정확히 1회 호출함.
 * </ul>
 */
class SkillCombatVisitorPropertyTest {

    private static final int RANK_COUNT = 16;
    private final SkillDamagePolicy policy = new SkillDamagePolicy();

    /** Property 1 (동치성): 8종 서브타입의 모든 값 다형 메서드는 기존 기준 계산과 정확히 동일해야 한다. */
    @Property(tries = 100)
    void should_matchLegacyCalculations_forAllSubtypesAndRanks(
            @ForAll("allSkills") final Skill skill, @ForAll final SkillRank rank) {

        // 1. primaryStatLabel 검증
        final String expectedLabel =
                switch (skill) {
                    case DefenseSkill def -> "피해 경감";
                    case RecoverySkill rec -> "회복량";
                    case BuffSkill buf -> "흡수율";
                    case CcSkill cc -> "성공률";
                    case DotSkill dot -> "초기 배율";
                    case PassiveSkill pas -> "패시브 효과";
                    case DamageSkill dam -> "보너스 데미지";
                    case UltimateSkill ult -> "보너스 데미지";
                };
        assertThat(skill.primaryStatLabel()).isEqualTo(expectedLabel);

        // 2. primaryValueAt 검증
        final int expectedPrimaryValue =
                switch (skill) {
                    case DamageSkill dam -> policy.multiplier(dam, rank);
                    case DefenseSkill def -> policy.blockRate(def, rank);
                    case RecoverySkill rec -> rec.healAmountAt(rank);
                    case UltimateSkill ult -> ult.multiplierAt(rank);
                    case BuffSkill buf -> buf.absorbRateAt(rank);
                    case CcSkill cc -> cc.successRateAt(rank);
                    case DotSkill dot -> dot.initialMultiplierAt(rank);
                    case PassiveSkill pas -> 0;
                };
        assertThat(skill.primaryValueAt(rank)).isEqualTo(expectedPrimaryValue);

        // 3. resourceCostAt 검증
        final int expectedResourceCost =
                switch (skill) {
                    case DefenseSkill def -> def.resourceCostAt(rank);
                    case RecoverySkill rec -> rec.resourceCostAt(rank);
                    default -> skill.resourceCost();
                };
        assertThat(skill.resourceCostAt(rank)).isEqualTo(expectedResourceCost);

        // 4. counterValueAt 검증
        final Optional<Integer> expectedCounter =
                switch (skill) {
                    case DefenseSkill def -> {
                        final int mult = policy.counterMultiplier(def, rank);
                        yield mult > 0 ? Optional.of(mult) : Optional.empty();
                    }
                    default -> Optional.empty();
                };
        assertThat(skill.counterValueAt(rank)).isEqualTo(expectedCounter);

        // 5. critBonusValueAt 검증
        final Optional<Integer> expectedCritBonus =
                switch (skill) {
                    case DefenseSkill def ->
                            (def.critBonusByRank() != null && !def.critBonusByRank().isEmpty())
                                    ? Optional.of(def.critBonusAt(rank))
                                    : Optional.empty();
                    default -> Optional.empty();
                };
        assertThat(skill.critBonusValueAt(rank)).isEqualTo(expectedCritBonus);
    }

    /** Property 2 (디스패치 정합성): skill.accept(visitor)는 정확히 해당 서브타입 visit 메서드를 1회 호출해야 한다. */
    @Property(tries = 100)
    void should_dispatchToCorrectVisitorMethod_forAllSubtypes(
            @ForAll("allSkills") final Skill skill) {

        final DispatchRecordingVisitor visitor = new DispatchRecordingVisitor();
        final String dispatchedType = skill.accept(visitor);

        final String expectedType =
                switch (skill) {
                    case DamageSkill dam -> "DamageSkill";
                    case DefenseSkill def -> "DefenseSkill";
                    case RecoverySkill rec -> "RecoverySkill";
                    case UltimateSkill ult -> "UltimateSkill";
                    case PassiveSkill pas -> "PassiveSkill";
                    case BuffSkill buf -> "BuffSkill";
                    case CcSkill cc -> "CcSkill";
                    case DotSkill dot -> "DotSkill";
                };

        assertThat(dispatchedType).isEqualTo(expectedType);
        assertThat(visitor.totalVisits).isEqualTo(1);
    }

    // ─── Recording Visitor ──────────────────────────────────────────────────

    private static class DispatchRecordingVisitor implements SkillCombatVisitor<String> {
        private int totalVisits = 0;

        @Override
        public String visitDamage(final DamageSkill skill) {
            totalVisits++;
            return "DamageSkill";
        }

        @Override
        public String visitDefense(final DefenseSkill skill) {
            totalVisits++;
            return "DefenseSkill";
        }

        @Override
        public String visitRecovery(final RecoverySkill skill) {
            totalVisits++;
            return "RecoverySkill";
        }

        @Override
        public String visitUltimate(final UltimateSkill skill) {
            totalVisits++;
            return "UltimateSkill";
        }

        @Override
        public String visitPassive(final PassiveSkill skill) {
            totalVisits++;
            return "PassiveSkill";
        }

        @Override
        public String visitBuff(final BuffSkill skill) {
            totalVisits++;
            return "BuffSkill";
        }

        @Override
        public String visitCc(final CcSkill skill) {
            totalVisits++;
            return "CcSkill";
        }

        @Override
        public String visitDot(final DotSkill skill) {
            totalVisits++;
            return "DotSkill";
        }
    }

    // ─── Arbitraries ────────────────────────────────────────────────────────

    @Provide
    Arbitrary<Skill> allSkills() {
        return Arbitraries.oneOf(
                damageSkills(),
                defenseSkills(),
                recoverySkills(),
                ultimateSkills(),
                buffSkills(),
                ccSkills(),
                dotSkills(),
                passiveSkills());
    }

    @Provide
    Arbitrary<DamageSkill> damageSkills() {
        return Combinators.combine(
                        Arbitraries.of(SkillType.NORMAL, SkillType.HEAVY, SkillType.DEBUFF),
                        Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.MAGIC),
                        monotoneRankMap(),
                        Arbitraries.integers().between(1, 20),
                        Arbitraries.integers().between(0, 100))
                .as(
                        (type, talent, map, cost, crit) ->
                                new DamageSkill(
                                        "dmg-" + cost,
                                        "딜스킬",
                                        type,
                                        talent,
                                        cost,
                                        map,
                                        "설명",
                                        1,
                                        crit));
    }

    @Provide
    Arbitrary<DefenseSkill> defenseSkills() {
        return Combinators.combine(
                        monotoneRankMap(),
                        monotoneRankMap(),
                        monotoneRankMap(),
                        monotoneRankMap(),
                        Arbitraries.integers().between(1, 15))
                .as(
                        (blockMap, counterMap, costMap, critMap, cost) ->
                                new DefenseSkill(
                                        "def-" + cost,
                                        "방어스킬",
                                        SkillType.DEFENSE,
                                        SkillTalent.COMMON,
                                        cost,
                                        blockMap,
                                        counterMap,
                                        "설명",
                                        costMap,
                                        critMap));
    }

    @Provide
    Arbitrary<RecoverySkill> recoverySkills() {
        return Combinators.combine(
                        monotoneRankMap(), monotoneRankMap(), Arbitraries.integers().between(1, 20))
                .as(
                        (healMap, costMap, cost) ->
                                new RecoverySkill(
                                        "rec-" + cost,
                                        "회복스킬",
                                        SkillType.RECOVERY,
                                        SkillTalent.MAGIC,
                                        cost,
                                        healMap,
                                        costMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<UltimateSkill> ultimateSkills() {
        return Combinators.combine(
                        monotoneRankMap(),
                        monotoneRankMap(),
                        monotoneRankMap(),
                        Arbitraries.integers().between(1, 30),
                        Arbitraries.integers().between(0, 100))
                .as(
                        (multMap, hitMap, coolMap, cost, crit) ->
                                new UltimateSkill(
                                        "ult-" + cost,
                                        "궁극기",
                                        SkillType.ULTIMATE,
                                        SkillTalent.MELEE,
                                        cost,
                                        multMap,
                                        hitMap,
                                        crit,
                                        coolMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<BuffSkill> buffSkills() {
        return Combinators.combine(
                        monotoneRankMap(),
                        Arbitraries.integers().between(1, 5),
                        Arbitraries.integers().between(1, 20))
                .as(
                        (absorbMap, duration, cost) ->
                                new BuffSkill(
                                        "buf-" + cost,
                                        "버프스킬",
                                        SkillType.BUFF,
                                        SkillTalent.MAGIC,
                                        cost,
                                        duration,
                                        absorbMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<CcSkill> ccSkills() {
        return Combinators.combine(monotoneRankMap(), Arbitraries.integers().between(1, 20))
                .as(
                        (successMap, cost) ->
                                new CcSkill(
                                        "cc-" + cost,
                                        "CC스킬",
                                        SkillType.CC,
                                        SkillTalent.ARCHERY,
                                        cost,
                                        successMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<DotSkill> dotSkills() {
        return Combinators.combine(
                        monotoneRankMap(),
                        monotoneRankMap(),
                        monotoneRankMap(),
                        Arbitraries.integers().between(1, 20))
                .as(
                        (initMap, dotMap, turnsMap, cost) ->
                                new DotSkill(
                                        "dot-" + cost,
                                        "도트스킬",
                                        SkillType.DOT,
                                        SkillTalent.ARCHERY,
                                        cost,
                                        initMap,
                                        dotMap,
                                        turnsMap,
                                        "설명"));
    }

    @Provide
    Arbitrary<PassiveSkill> passiveSkills() {
        return Arbitraries.of(
                new PassiveSkill(
                        "pas-1",
                        "패시브1",
                        SkillType.PASSIVE,
                        SkillTalent.MELEE,
                        0,
                        Map.of(BonusTarget.STR, 20),
                        "설명"),
                new PassiveSkill(
                        "pas-2",
                        "패시브2",
                        SkillType.PASSIVE,
                        SkillTalent.MAGIC,
                        0,
                        Map.of(BonusTarget.MP, 30, BonusTarget.MP_REGEN, 5),
                        "설명"));
    }

    private Arbitrary<Map<SkillRank, Integer>> monotoneRankMap() {
        final Arbitrary<Integer> baseArb = Arbitraries.integers().between(1, 500);
        final Arbitrary<int[]> incrementsArb =
                Arbitraries.integers().between(0, 50).array(int[].class).ofSize(RANK_COUNT - 1);

        return Combinators.combine(baseArb, incrementsArb)
                .as(
                        (base, increments) -> {
                            final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
                            final SkillRank[] ranks = SkillRank.values();
                            int current = base;
                            map.put(ranks[0], current);
                            for (int i = 1; i < RANK_COUNT; i++) {
                                current += increments[i - 1];
                                map.put(ranks[i], current);
                            }
                            return map;
                        });
    }
}
