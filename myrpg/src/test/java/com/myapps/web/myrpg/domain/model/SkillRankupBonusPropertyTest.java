package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link SkillRankupBonus}의 합산 로직이 재능별 주 스탯에만 {@code Σ(order × 1)}을 가산하고, Critical·바이탈 계열은 항상 0인지
 * 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 보유 스킬 집합(각각 임의 재능·랭크)을 생성하고, 계산 결과가 수식과 일치하는지 확인한다. F(order 0)는 0 기여, MASTER(order 15)는 15
 * 기여.
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4</b>
 */
class SkillRankupBonusPropertyTest {

    // Feature: 005-skill-system, Property 12: 랭크업 영구 보너스 합산

    private final SkillRankupBonus bonus = new SkillRankupBonus();

    /**
     * 임의의 보유 스킬 목록에 대해 합산된 STR/DEX/INT/DEF가 각 재능별 order 합과 일치하는지 검증한다.
     *
     * <p>각 스킬은 고유 ID를 가지므로 lookup 함수에서 재능 충돌이 없다.
     *
     * @param entries 임의 생성된 (skillId, talent, rank) 목록 (ID 유일)
     */
    @Property(tries = 100)
    void should_sumOrderByTalent_when_anyOwnedSkills(
            @ForAll("uniqueSkillEntries") final List<SkillEntry> entries) {

        final List<CharacterSkill> owned =
                entries.stream()
                        .map(entry -> new CharacterSkill(1L, entry.skillId(), entry.rank(), 0, 0))
                        .toList();

        final Function<String, Optional<Skill>> lookup = buildLookup(entries);

        final Stats result = bonus.sum(owned, lookup);

        int expectedStr = 0;
        int expectedDex = 0;
        int expectedInt = 0;
        int expectedDef = 0;

        for (final SkillEntry entry : entries) {
            final int order = entry.rank().order();
            switch (entry.talent()) {
                case MELEE -> expectedStr += order;
                case ARCHERY -> expectedDex += order;
                case MAGIC -> expectedInt += order;
                case COMMON -> expectedDef += order;
            }
        }

        assertThat(result.str()).isEqualTo(expectedStr);
        assertThat(result.dex()).isEqualTo(expectedDex);
        assertThat(result.intelligence()).isEqualTo(expectedInt);
        assertThat(result.defense()).isEqualTo(expectedDef);
    }

    /**
     * 임의의 보유 스킬 목록에 대해 Critical이 항상 0인지 검증한다.
     *
     * @param entries 임의 생성된 (skillId, talent, rank) 목록 (ID 유일)
     */
    @Property(tries = 100)
    void should_haveCriticalZero_when_anyOwnedSkills(
            @ForAll("uniqueSkillEntries") final List<SkillEntry> entries) {

        final List<CharacterSkill> owned =
                entries.stream()
                        .map(entry -> new CharacterSkill(1L, entry.skillId(), entry.rank(), 0, 0))
                        .toList();

        final Function<String, Optional<Skill>> lookup = buildLookup(entries);

        final Stats result = bonus.sum(owned, lookup);

        assertThat(result.critical()).isZero();
    }

    /**
     * F 랭크(order 0) 스킬만 있으면 모든 스탯이 0인지 검증한다.
     *
     * @param talents 임의의 재능 목록
     */
    @Property(tries = 100)
    void should_contributeZero_when_allRanksAreF(
            @ForAll("talentList") final List<SkillTalent> talents) {

        final List<SkillEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < talents.size(); i++) {
            entries.add(new SkillEntry("skill_" + i, talents.get(i), SkillRank.F));
        }

        final List<CharacterSkill> owned =
                entries.stream()
                        .map(entry -> new CharacterSkill(1L, entry.skillId(), entry.rank(), 0, 0))
                        .toList();

        final Function<String, Optional<Skill>> lookup = buildLookup(entries);

        final Stats result = bonus.sum(owned, lookup);

        assertThat(result).isEqualTo(Stats.ZERO);
    }

    /**
     * MASTER 랭크(order 15) 스킬은 15를 기여하는지 검증한다.
     *
     * @param talent 임의의 재능
     */
    @Property(tries = 100)
    void should_contribute15_when_rankIsMaster(@ForAll("anyTalent") final SkillTalent talent) {
        final SkillEntry entry = new SkillEntry("master_skill", talent, SkillRank.MASTER);
        final List<CharacterSkill> owned =
                List.of(new CharacterSkill(1L, entry.skillId(), entry.rank(), 0, 0));

        final Function<String, Optional<Skill>> lookup = buildLookup(List.of(entry));

        final Stats result = bonus.sum(owned, lookup);

        final int expectedValue = 15;
        switch (talent) {
            case MELEE -> assertThat(result.str()).isEqualTo(expectedValue);
            case ARCHERY -> assertThat(result.dex()).isEqualTo(expectedValue);
            case MAGIC -> assertThat(result.intelligence()).isEqualTo(expectedValue);
            case COMMON -> assertThat(result.defense()).isEqualTo(expectedValue);
        }
    }

    /**
     * defense 스킬은 임의의 랭크에서 DEF = order, HP = order * 5를 기여함을 검증한다.
     *
     * @param rank 임의의 스킬 랭크
     */
    @Property(tries = 50)
    void should_contributeDefAndHp_when_defenseSkill(@ForAll("anyRank") final SkillRank rank) {
        final CharacterSkill defense = new CharacterSkill(1L, "defense", rank, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                id ->
                        "defense".equals(id)
                                ? Optional.of(createDamageSkill("defense", SkillTalent.COMMON))
                                : Optional.empty();

        final Stats statResult = bonus.sum(List.of(defense), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(defense), lookup);

        assertThat(statResult.defense()).isEqualTo(rank.order());
        assertThat(statResult.str()).isZero();
        assertThat(statResult.dex()).isZero();
        assertThat(statResult.intelligence()).isZero();
        assertThat(statResult.critical()).isZero();

        assertThat(vitalResult.hp()).isEqualTo(rank.order() * 5);
        assertThat(vitalResult.mp()).isZero();
        assertThat(vitalResult.stamina()).isZero();
    }

    /**
     * counter_attack 스킬은 임의의 랭크에서 스탯 및 HP 기여가 0임을 검증한다.
     *
     * @param rank 임의의 스킬 랭크
     */
    @Property(tries = 50)
    void should_contributeZero_when_counterAttackSkill(@ForAll("anyRank") final SkillRank rank) {
        final CharacterSkill counter = new CharacterSkill(1L, "counter_attack", rank, 0, 0);
        final Function<String, Optional<Skill>> lookup =
                id ->
                        "counter_attack".equals(id)
                                ? Optional.of(
                                        createDamageSkill("counter_attack", SkillTalent.COMMON))
                                : Optional.empty();

        final Stats statResult = bonus.sum(List.of(counter), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(counter), lookup);

        assertThat(statResult).isEqualTo(Stats.ZERO);
        assertThat(vitalResult).isEqualTo(new VitalMax(0, 0, 0));
    }

    /**
     * Property 6: 임의의 패시브 스킬과 임의의 랭크(order k)에 대해 합산된 보너스는 round(Max * k / 15) 불변식을 만족한다.
     *
     * <p><b>Validates: Requirement 6.3</b>
     *
     * @param maxStat MASTER 랭크 기준 최대 보너스 (1~200)
     * @param rank 임의의 스킬 랭크
     */
    @Property(tries = 100)
    void property6_should_accumulateLinearBonus_for_passiveSkill(
            @ForAll("positiveMaxStat") final int maxStat, @ForAll("anyRank") final SkillRank rank) {
        final CharacterSkill passiveOwned = new CharacterSkill(1L, "test_passive", rank, 0, 0);
        final PassiveSkill passiveSkill =
                new PassiveSkill(
                        "test_passive",
                        "테스트 패시브",
                        SkillType.PASSIVE,
                        SkillTalent.COMMON,
                        0,
                        Map.of(BonusTarget.STR, maxStat, BonusTarget.HP, maxStat),
                        "desc");

        final Function<String, Optional<Skill>> lookup =
                id -> "test_passive".equals(id) ? Optional.of(passiveSkill) : Optional.empty();

        final Stats statResult = bonus.sum(List.of(passiveOwned), lookup);
        final VitalMax vitalResult = bonus.sumVital(List.of(passiveOwned), lookup);

        final int expectedBonus = Math.round((float) maxStat * rank.order() / 15.0f);
        assertThat(statResult.str()).isEqualTo(expectedBonus);
        assertThat(vitalResult.hp()).isEqualTo(expectedBonus);
    }

    @Provide
    Arbitrary<Integer> positiveMaxStat() {
        return Arbitraries.integers().between(1, 200);
    }

    @Provide
    Arbitrary<SkillRank> anyRank() {
        return Arbitraries.of(SkillRank.values());
    }

    /**
     * 고유 skillId를 가진 보유 스킬 엔트리 목록을 생성하는 Arbitrary 제공자.
     *
     * <p>각 엔트리에 순차 번호 기반 고유 ID를 부여하여 lookup 충돌을 방지한다.
     *
     * @return 0~10개의 SkillEntry 목록(ID 유일)을 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<List<SkillEntry>> uniqueSkillEntries() {
        final Arbitrary<Integer> sizeArbitrary = Arbitraries.integers().between(0, 10);
        return sizeArbitrary.flatMap(this::buildEntriesForSize);
    }

    private Arbitrary<List<SkillEntry>> buildEntriesForSize(final int size) {
        if (size == 0) {
            return Arbitraries.just(List.of());
        }
        final Arbitrary<List<SkillTalent>> talents =
                Arbitraries.of(SkillTalent.values()).list().ofSize(size);
        final Arbitrary<List<SkillRank>> ranks =
                Arbitraries.of(SkillRank.values()).list().ofSize(size);
        return Combinators.combine(talents, ranks)
                .as(
                        (talentList, rankList) -> {
                            final List<SkillEntry> result = new java.util.ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                result.add(
                                        new SkillEntry(
                                                "skill_" + i, talentList.get(i), rankList.get(i)));
                            }
                            return List.copyOf(result);
                        });
    }

    /**
     * SkillTalent 목록을 생성하는 Arbitrary 제공자.
     *
     * @return 0~10개의 SkillTalent 목록을 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<List<SkillTalent>> talentList() {
        return Arbitraries.of(SkillTalent.values()).list().ofMinSize(0).ofMaxSize(10);
    }

    /**
     * 임의의 SkillTalent를 하나 선택하는 Arbitrary 제공자.
     *
     * @return SkillTalent 하나를 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> anyTalent() {
        return Arbitraries.of(SkillTalent.values());
    }

    /**
     * SkillEntry 목록으로부터 lookup 함수를 생성한다.
     *
     * @param entries 스킬 엔트리 목록
     * @return skillId로 Skill을 조회하는 함수
     */
    private Function<String, Optional<Skill>> buildLookup(final List<SkillEntry> entries) {
        final Map<String, SkillTalent> talentMap = new java.util.HashMap<>();
        for (final SkillEntry entry : entries) {
            talentMap.put(entry.skillId(), entry.talent());
        }
        return id -> {
            final SkillTalent talent = talentMap.get(id);
            if (talent == null) {
                return Optional.empty();
            }
            return Optional.of(createDamageSkill(id, talent));
        };
    }

    /**
     * 테스트용 DamageSkill을 생성한다.
     *
     * @param id 스킬 ID
     * @param talent 재능
     * @return 더미 multiplierByRank를 가진 DamageSkill
     */
    private DamageSkill createDamageSkill(final String id, final SkillTalent talent) {
        final Map<SkillRank, Integer> multiplierMap = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            multiplierMap.put(rank, 100 + rank.order() * 10);
        }
        return new DamageSkill(id, id, SkillType.NORMAL, talent, 10, multiplierMap, "test");
    }

    /**
     * 프로퍼티 테스트 입력을 표현하는 내부 record.
     *
     * @param skillId 스킬 ID
     * @param talent 재능 분류
     * @param rank 랭크
     */
    record SkillEntry(String skillId, SkillTalent talent, SkillRank rank) {}
}
