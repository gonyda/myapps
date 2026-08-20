package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
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
 * <p>실제 {@code data/skill.json}을 로드하여 9개 딜 스킬의 {@code hitCount}/{@code critBonus}가 §4 확정표와 일치하고, 마법
 * 스킬 {@code critBonus == 0}, 모든 스킬 {@code critBonus ≤ 100}, 모든 {@code multiplierByRank}가 16키·단조
 * 비감소, 다단 총 배율이 밴드 내임을 검증한다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 10: skill.json 데이터 규격
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.4, 3.5, 3.6, 12.2, 12.4, 12.5</b>
 */
class SkillCatalogDataConformancePropertyTest {

    private static final int EXPECTED_DAMAGE_SKILL_COUNT = 9;
    private static final int EXPECTED_RANK_MAP_SIZE = 16;
    private static final int MAX_CRIT_BONUS = 100;

    // §4 확정표: 3타 총 배율 밴드
    private static final int THREE_HIT_TOTAL_F_MIN = 105;
    private static final int THREE_HIT_TOTAL_MASTER_MAX = 195;

    // §4 확정표: 4타 총 배율 밴드
    private static final int FOUR_HIT_TOTAL_F_MIN = 108;
    private static final int FOUR_HIT_TOTAL_MASTER_MAX = 200;

    private final List<DamageSkill> damageSkills;

    SkillCatalogDataConformancePropertyTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final SkillCatalogService service = new SkillCatalogService(objectMapper);
        final InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("data/skill.json");
        final List<Skill> allSkills = service.loadFromStream(inputStream);
        this.damageSkills =
                allSkills.stream()
                        .filter(DamageSkill.class::isInstance)
                        .map(DamageSkill.class::cast)
                        .toList();
    }

    /** 딜 스킬이 정확히 9개 로드되는지 검증한다. */
    @Example
    void should_loadNineDamageSkills_when_skillJsonParsed() {
        assertThat(damageSkills).hasSize(EXPECTED_DAMAGE_SKILL_COUNT);
    }

    /**
     * 임의의 딜 스킬에 대해 hitCount와 critBonus가 §4 확정표와 일치하는지 검증한다.
     *
     * @param skill 로드된 딜 스킬 중 하나
     */
    @Property(tries = 100)
    void should_matchConfirmedTable_when_damageSkillLoaded(
            @ForAll("loadedDamageSkill") final DamageSkill skill) {
        final ExpectedSpec expected = expectedSpecFor(skill.id());
        assertThat(expected).as("알 수 없는 딜 스킬 id: %s", skill.id()).isNotNull();

        assertThat(skill.hitCount())
                .as("스킬 '%s'의 hitCount가 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.hitCount());
        assertThat(skill.critBonus())
                .as("스킬 '%s'의 critBonus가 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.critBonus());
        assertThat(skill.type())
                .as("스킬 '%s'의 type이 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.type());
        assertThat(skill.talent())
                .as("스킬 '%s'의 talent가 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.talent());
        assertThat(skill.multiplierByRank().get(SkillRank.F))
                .as("스킬 '%s'의 F랭크 배율이 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.perHitF());
        assertThat(skill.multiplierByRank().get(SkillRank.MASTER))
                .as("스킬 '%s'의 MASTER랭크 배율이 확정표와 일치해야 합니다", skill.id())
                .isEqualTo(expected.perHitMaster());
    }

    /**
     * 임의의 마법 딜 스킬에 대해 critBonus가 반드시 0임을 검증한다.
     *
     * @param skill 로드된 딜 스킬 중 하나
     */
    @Property(tries = 100)
    void should_haveCritBonusZero_when_magicDamageSkill(
            @ForAll("loadedDamageSkill") final DamageSkill skill) {
        if (skill.talent() == SkillTalent.MAGIC) {
            assertThat(skill.critBonus())
                    .as("마법 스킬 '%s'의 critBonus는 0이어야 합니다", skill.id())
                    .isZero();
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

    /**
     * 임의의 딜 스킬에 대해 multiplierByRank가 16키 완비이고 단조 비감소임을 검증한다.
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
     * 다단 스킬(hitCount ≥ 2)의 총 배율(per-hit × hitCount)이 명시 밴드 안임을 검증한다. 3타: F 105→MASTER 195, 4타: F
     * 108→MASTER 200.
     *
     * @param skill 로드된 딜 스킬 중 하나
     */
    @Property(tries = 100)
    void should_haveTotalMultiplierInBand_when_multiHitSkill(
            @ForAll("loadedDamageSkill") final DamageSkill skill) {
        final int hitCount = skill.hitCount();
        if (hitCount < 2) {
            return;
        }

        final int totalF = skill.multiplierByRank().get(SkillRank.F) * hitCount;
        final int totalMaster = skill.multiplierByRank().get(SkillRank.MASTER) * hitCount;

        if (hitCount == 3) {
            assertThat(totalF)
                    .as(
                            "3타 스킬 '%s'의 총 F배율(%d)이 밴드 최솟값(%d) 이상이어야 합니다",
                            skill.id(), totalF, THREE_HIT_TOTAL_F_MIN)
                    .isGreaterThanOrEqualTo(THREE_HIT_TOTAL_F_MIN);
            assertThat(totalMaster)
                    .as(
                            "3타 스킬 '%s'의 총 MASTER배율(%d)이 밴드 최댓값(%d) 이하여야 합니다",
                            skill.id(), totalMaster, THREE_HIT_TOTAL_MASTER_MAX)
                    .isLessThanOrEqualTo(THREE_HIT_TOTAL_MASTER_MAX);
        } else if (hitCount == 4) {
            assertThat(totalF)
                    .as(
                            "4타 스킬 '%s'의 총 F배율(%d)이 밴드 최솟값(%d) 이상이어야 합니다",
                            skill.id(), totalF, FOUR_HIT_TOTAL_F_MIN)
                    .isGreaterThanOrEqualTo(FOUR_HIT_TOTAL_F_MIN);
            assertThat(totalMaster)
                    .as(
                            "4타 스킬 '%s'의 총 MASTER배율(%d)이 밴드 최댓값(%d) 이하여야 합니다",
                            skill.id(), totalMaster, FOUR_HIT_TOTAL_MASTER_MAX)
                    .isLessThanOrEqualTo(FOUR_HIT_TOTAL_MASTER_MAX);
        }
    }

    // ── Providers ──

    /**
     * 로드된 딜 스킬 목록에서 임의로 하나를 선택하는 Arbitrary를 제공한다.
     *
     * @return 딜 스킬 Arbitrary
     */
    @Provide
    Arbitrary<DamageSkill> loadedDamageSkill() {
        return Arbitraries.of(damageSkills);
    }

    // ── Expected Spec Table ──

    /**
     * §4 확정표에 기반한 기대 스펙을 스킬 ID로 조회한다.
     *
     * @param skillId 조회할 스킬 ID
     * @return 기대 스펙, 미존재 시 {@code null}
     */
    private ExpectedSpec expectedSpecFor(final String skillId) {
        return switch (skillId) {
            case "slash" -> new ExpectedSpec(SkillTalent.MELEE, SkillType.NORMAL, 1, 0, 90, 170);
            case "windmill" -> new ExpectedSpec(SkillTalent.MELEE, SkillType.NORMAL, 3, 0, 35, 65);
            case "smash" -> new ExpectedSpec(SkillTalent.MELEE, SkillType.HEAVY, 1, 80, 130, 250);
            case "aimed_shot" ->
                    new ExpectedSpec(SkillTalent.ARCHERY, SkillType.NORMAL, 1, 0, 90, 170);
            case "arrow_revolver" ->
                    new ExpectedSpec(SkillTalent.ARCHERY, SkillType.NORMAL, 4, 0, 27, 50);
            case "magnum_shot" ->
                    new ExpectedSpec(SkillTalent.ARCHERY, SkillType.HEAVY, 1, 100, 140, 260);
            case "mana_bolt" ->
                    new ExpectedSpec(SkillTalent.MAGIC, SkillType.NORMAL, 1, 0, 90, 170);
            case "icebolt" -> new ExpectedSpec(SkillTalent.MAGIC, SkillType.NORMAL, 3, 0, 35, 65);
            case "firebolt" -> new ExpectedSpec(SkillTalent.MAGIC, SkillType.HEAVY, 1, 0, 130, 250);
            default -> null;
        };
    }

    /**
     * §4 확정표의 기대 스펙을 나타내는 내부 record.
     *
     * @param talent 기대 재능
     * @param type 기대 스킬 타입
     * @param hitCount 기대 히트 수
     * @param critBonus 기대 크리 보너스
     * @param perHitF 기대 F랭크 히트당 배율
     * @param perHitMaster 기대 MASTER랭크 히트당 배율
     */
    private record ExpectedSpec(
            SkillTalent talent,
            SkillType type,
            int hitCount,
            int critBonus,
            int perHitF,
            int perHitMaster) {}
}
