package com.myapps.web.myrpg.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 {@code data/skill.json} 로딩 통합 테스트.
 *
 * <p>Spring Boot 컨텍스트 전체를 기동하여 {@link SkillCatalogService}가
 * 클래스패스 리소스를 정상 로드하고, 7종 스킬·16키 랭크맵·유일 id를 검증한다.
 *
 * <p>Validates: Requirements 1.1, 1.7
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SkillCatalogLoadIntegrationTest {

    private static final int TOTAL_SKILL_COUNT = 7;
    private static final int EXPECTED_RANK_MAP_SIZE = 16;
    private static final Set<String> KNOWN_SKILL_IDS = Set.of(
            "smash", "windmill", "magnum_shot", "arrow_revolver",
            "firebolt", "icebolt", "defense");

    private final SkillCatalogService skillCatalogService;

    SkillCatalogLoadIntegrationTest(final SkillCatalogService skillCatalogService) {
        this.skillCatalogService = skillCatalogService;
    }

    /**
     * 전체 스킬 수가 7개인지 검증한다.
     */
    @Test
    void should_loadSevenSkills_when_applicationStarts() {
        final List<Skill> allSkills = skillCatalogService.all();

        assertThat(allSkills).hasSize(TOTAL_SKILL_COUNT);
    }

    /**
     * 모든 스킬 id가 유일한지 검증한다.
     */
    @Test
    void should_haveUniqueIds_forAllSkills() {
        final List<Skill> allSkills = skillCatalogService.all();
        final Set<String> ids = new HashSet<>();

        for (final Skill skill : allSkills) {
            assertThat(ids.add(skill.id()))
                    .as("중복 id가 존재합니다: " + skill.id())
                    .isTrue();
        }
    }

    /**
     * 알려진 7개 스킬 id가 모두 로드되었는지 검증한다.
     */
    @Test
    void should_containAllKnownSkillIds_when_loaded() {
        final List<Skill> allSkills = skillCatalogService.all();
        final Set<String> loadedIds = new HashSet<>();

        for (final Skill skill : allSkills) {
            loadedIds.add(skill.id());
        }

        assertThat(loadedIds).containsAll(KNOWN_SKILL_IDS);
    }

    /**
     * 딜스킬(DamageSkill)의 multiplierByRank가 16키를 완비하는지 검증한다.
     */
    @Test
    void should_haveSixteenRankKeys_forDamageSkillMultiplierByRank() {
        final List<Skill> allSkills = skillCatalogService.all();

        final List<DamageSkill> damageSkills = allSkills.stream()
                .filter(DamageSkill.class::isInstance)
                .map(DamageSkill.class::cast)
                .toList();

        assertThat(damageSkills).isNotEmpty();

        for (final DamageSkill damageSkill : damageSkills) {
            final Map<SkillRank, Integer> multiplierByRank = damageSkill.multiplierByRank();
            assertThat(multiplierByRank)
                    .as("스킬 '%s'의 multiplierByRank는 16키여야 합니다", damageSkill.id())
                    .hasSize(EXPECTED_RANK_MAP_SIZE);

            for (final SkillRank rank : SkillRank.values()) {
                assertThat(multiplierByRank)
                        .as("스킬 '%s'에 랭크 '%s' 키가 있어야 합니다", damageSkill.id(), rank.name())
                        .containsKey(rank);
            }
        }
    }

    /**
     * 디펜스 스킬(DefenseSkill)의 blockRateByRank와 counterMultiplierByRank가
     * 각각 16키를 완비하는지 검증한다.
     */
    @Test
    void should_haveSixteenRankKeys_forDefenseSkillMaps() {
        final List<Skill> allSkills = skillCatalogService.all();

        final List<DefenseSkill> defenseSkills = allSkills.stream()
                .filter(DefenseSkill.class::isInstance)
                .map(DefenseSkill.class::cast)
                .toList();

        assertThat(defenseSkills).isNotEmpty();

        for (final DefenseSkill defenseSkill : defenseSkills) {
            final Map<SkillRank, Integer> blockRateByRank = defenseSkill.blockRateByRank();
            final Map<SkillRank, Integer> counterMultiplierByRank = defenseSkill.counterMultiplierByRank();

            assertThat(blockRateByRank)
                    .as("스킬 '%s'의 blockRateByRank는 16키여야 합니다", defenseSkill.id())
                    .hasSize(EXPECTED_RANK_MAP_SIZE);
            assertThat(counterMultiplierByRank)
                    .as("스킬 '%s'의 counterMultiplierByRank는 16키여야 합니다", defenseSkill.id())
                    .hasSize(EXPECTED_RANK_MAP_SIZE);

            for (final SkillRank rank : SkillRank.values()) {
                assertThat(blockRateByRank)
                        .as("스킬 '%s'의 blockRateByRank에 '%s' 키가 있어야 합니다",
                                defenseSkill.id(), rank.name())
                        .containsKey(rank);
                assertThat(counterMultiplierByRank)
                        .as("스킬 '%s'의 counterMultiplierByRank에 '%s' 키가 있어야 합니다",
                                defenseSkill.id(), rank.name())
                        .containsKey(rank);
            }
        }
    }

    /**
     * byId로 알려진 스킬을 조회할 수 있는지 검증한다.
     */
    @Test
    void should_findSkillById_when_knownIdUsed() {
        for (final String skillId : KNOWN_SKILL_IDS) {
            assertThat(skillCatalogService.byId(skillId))
                    .as("스킬 '%s'가 byId로 조회되어야 합니다", skillId)
                    .isPresent();
        }
    }
}
