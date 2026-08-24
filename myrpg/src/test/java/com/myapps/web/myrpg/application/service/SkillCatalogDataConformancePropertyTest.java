package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.BuffSkill;
import com.myapps.web.myrpg.domain.model.CcSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.RecoverySkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.ObjectMapper;

/**
 * skill.json 데이터 규격 적합성 프로퍼티 테스트.
 *
 * <p>실제 {@code data/skill.json}을 로드하여 전체 35종(액티브 29종 + 패시브 6종) 스킬의 랭크 맵 16키 완비 및 단조 비감소성을 검증한다.
 *
 * <p><b>Validates: Requirements 1.3, 1.4 (Property 1)</b>
 */
class SkillCatalogDataConformancePropertyTest {

    private static final int EXPECTED_TOTAL_SKILL_COUNT = 35;
    private static final int EXPECTED_RANK_MAP_SIZE = 16;
    private static final int MAX_CRIT_BONUS = 100;

    private final List<Skill> allSkills;
    private final List<DamageSkill> damageSkills;

    SkillCatalogDataConformancePropertyTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final SkillCatalogService service = new SkillCatalogService(objectMapper);
        final InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("data/skill.json");
        this.allSkills = service.loadFromStream(inputStream);
        this.damageSkills =
                allSkills.stream()
                        .filter(DamageSkill.class::isInstance)
                        .map(DamageSkill.class::cast)
                        .toList();
    }

    /** 전체 스킬이 정확히 35개 로드되는지 검증한다. */
    @Example
    void should_loadAll35Skills_when_skillJsonParsed() {
        assertThat(allSkills).hasSize(EXPECTED_TOTAL_SKILL_COUNT);
    }

    /**
     * Property 1: 임의의 딜 스킬에 대해 multiplierByRank가 16키 완비이고 단조 비감소임을 검증한다.
     *
     * <p><b>Validates: Requirement 1.3</b>
     *
     * @param skill 로드된 딜 스킬 중 하나
     */
    @Property(tries = 100)
    void should_haveMonotonicallyNonDecreasingRankMap_when_anyDamageSkill(
            @ForAll("loadedDamageSkill") final DamageSkill skill) {
        final Map<SkillRank, Integer> rankMap = skill.multiplierByRank();

        assertThat(rankMap)
                .as("스킬 '%s'의 multiplierByRank는 16키여야 합니다", skill.id())
                .hasSize(EXPECTED_RANK_MAP_SIZE);

        int previousValue = 0;
        for (final SkillRank rank : SkillRank.values()) {
            final Integer currentValue = rankMap.get(rank);
            assertThat(currentValue)
                    .as("스킬 '%s'의 랭크 '%s' 키가 존재해야 합니다", skill.id(), rank.name())
                    .isNotNull();
            assertThat(currentValue)
                    .as(
                            "스킬 '%s'의 랭크 '%s'(%d)가 이전 랭크(%d)보다 작지 않아야 합니다",
                            skill.id(), rank.name(), currentValue, previousValue)
                    .isGreaterThanOrEqualTo(previousValue);
            previousValue = currentValue;
        }
    }

    /**
     * 임의의 딜 스킬에 대해 critBonus가 상한(100) 이하임을 검증한다.
     *
     * @param skill 로드된 딜 스킬 중 하나
     */
    @Property(tries = 100)
    void should_notExceedCritBonusCap_when_anyDamageSkill(
            @ForAll("loadedDamageSkill") final DamageSkill skill) {
        assertThat(skill.critBonus())
                .as("스킬 '%s'의 critBonus는 %d 이하여야 합니다", skill.id(), MAX_CRIT_BONUS)
                .isLessThanOrEqualTo(MAX_CRIT_BONUS);
    }

    /** 임의의 힐링 스킬에 대해 healAmountByRank가 16키 완비이고 단조 비감소임을 검증한다. */
    @Example
    void should_haveMonotonicRankMap_for_recoverySkills() {
        final List<RecoverySkill> recoverySkills =
                allSkills.stream()
                        .filter(RecoverySkill.class::isInstance)
                        .map(RecoverySkill.class::cast)
                        .toList();

        assertThat(recoverySkills).isNotEmpty();
        for (final RecoverySkill skill : recoverySkills) {
            assertRankMapMonotonic(skill.id(), "healAmountByRank", skill.healAmountByRank());
        }
    }

    /** 임의의 궁극기 스킬에 대해 multiplierByRank 및 coolWinsByRank가 유효함을 검증한다. */
    @Example
    void should_haveValidRankMap_for_ultimateSkills() {
        final List<UltimateSkill> ultimateSkills =
                allSkills.stream()
                        .filter(UltimateSkill.class::isInstance)
                        .map(UltimateSkill.class::cast)
                        .toList();

        assertThat(ultimateSkills).hasSize(3);
        for (final UltimateSkill skill : ultimateSkills) {
            assertRankMapMonotonic(skill.id(), "multiplierByRank", skill.multiplierByRank());
            assertThat(skill.coolWinsAt(SkillRank.F)).isEqualTo(30);
            assertThat(skill.coolWinsAt(SkillRank.MASTER)).isEqualTo(10);
        }
    }

    /** 임의의 버프 및 CC 스킬에 대해 랭크맵이 16키 완비이고 단조 비감소임을 검증한다. */
    @Example
    void should_haveMonotonicRankMap_for_buffAndCcSkills() {
        final List<BuffSkill> buffSkills =
                allSkills.stream()
                        .filter(BuffSkill.class::isInstance)
                        .map(BuffSkill.class::cast)
                        .toList();
        for (final BuffSkill skill : buffSkills) {
            assertRankMapMonotonic(skill.id(), "absorbRateByRank", skill.absorbRateByRank());
        }

        final List<CcSkill> ccSkills =
                allSkills.stream()
                        .filter(CcSkill.class::isInstance)
                        .map(CcSkill.class::cast)
                        .toList();
        for (final CcSkill skill : ccSkills) {
            assertRankMapMonotonic(skill.id(), "successRateByRank", skill.successRateByRank());
        }
    }

    /** 패시브 6종이 유효한 스탯 보너스 맵을 가지는지 검증한다. */
    @Example
    void should_haveValidTotalStatBonus_for_passiveSkills() {
        final List<PassiveSkill> passives =
                allSkills.stream()
                        .filter(PassiveSkill.class::isInstance)
                        .map(PassiveSkill.class::cast)
                        .toList();

        assertThat(passives).hasSize(6);
        for (final PassiveSkill skill : passives) {
            assertThat(skill.totalStatBonus()).isNotEmpty();
        }
    }

    private void assertRankMapMonotonic(
            final String skillId, final String mapName, final Map<SkillRank, Integer> map) {
        assertThat(map).as("스킬 '%s'의 %s 맵 크기는 16이어야 합니다", skillId, mapName).hasSize(16);
        int previous = 0;
        for (final SkillRank rank : SkillRank.values()) {
            final Integer val = map.get(rank);
            assertThat(val).as("스킬 '%s'의 %s 랭크 %s 키가 존재해야 합니다", skillId, mapName, rank).isNotNull();
            assertThat(val)
                    .as(
                            "스킬 '%s'의 %s 랭크 %s(%d)가 이전(%d)보다 작지 않아야 합니다",
                            skillId, mapName, rank, val, previous)
                    .isGreaterThanOrEqualTo(previous);
            previous = val;
        }
    }

    // ── Providers ──

    @Provide
    Arbitrary<DamageSkill> loadedDamageSkill() {
        return Arbitraries.of(damageSkills);
    }
}
