package com.myapps.web.myrpg.domain.model;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DamageSkill} record의 생성자 및 접근자를 검증하는 단위 테스트.
 *
 * <p>7-인자 하위호환 보조 생성자가 hitCount=1, critBonus=0을 기본 설정하는지,
 * 9-인자 정규 생성자가 모든 필드를 올바르게 보존하는지 확인한다.
 */
class DamageSkillTest {

    private static final String SKILL_ID = "slash";
    private static final String SKILL_LABEL = "슬래시";
    private static final SkillType SKILL_TYPE = SkillType.NORMAL;
    private static final SkillTalent SKILL_TALENT = SkillTalent.MELEE;
    private static final int RESOURCE_COST = 5;
    private static final String SKILL_DESCRIPTION = "기본 근접 공격";

    /**
     * 7-인자 보조 생성자로 생성 시 hitCount는 1, critBonus는 0이 된다.
     */
    @Test
    @DisplayName("7-인자 보조 생성자: hitCount=1, critBonus=0 기본값")
    void should_setDefaultHitCountAndCritBonus_when_usingSevenArgConstructor() {
        final Map<SkillRank, Integer> multiplierByRank = buildMonotonicRankMap(90, 5);

        final DamageSkill skill = new DamageSkill(
                SKILL_ID, SKILL_LABEL, SKILL_TYPE, SKILL_TALENT,
                RESOURCE_COST, multiplierByRank, SKILL_DESCRIPTION);

        assertThat(skill.id()).isEqualTo(SKILL_ID);
        assertThat(skill.label()).isEqualTo(SKILL_LABEL);
        assertThat(skill.type()).isEqualTo(SKILL_TYPE);
        assertThat(skill.talent()).isEqualTo(SKILL_TALENT);
        assertThat(skill.resourceCost()).isEqualTo(RESOURCE_COST);
        assertThat(skill.multiplierByRank()).isEqualTo(multiplierByRank);
        assertThat(skill.description()).isEqualTo(SKILL_DESCRIPTION);
        assertThat(skill.hitCount()).isEqualTo(1);
        assertThat(skill.critBonus()).isEqualTo(0);
    }

    /**
     * 9-인자 정규 생성자로 생성 시 hitCount와 critBonus가 지정 값으로 설정된다.
     */
    @Test
    @DisplayName("9-인자 정규 생성자: hitCount·critBonus 명시")
    void should_preserveHitCountAndCritBonus_when_usingNineArgConstructor() {
        final Map<SkillRank, Integer> multiplierByRank = buildMonotonicRankMap(35, 2);
        final int hitCount = 3;
        final int critBonus = 80;

        final DamageSkill skill = new DamageSkill(
                SKILL_ID, SKILL_LABEL, SkillType.HEAVY, SkillTalent.ARCHERY,
                RESOURCE_COST, multiplierByRank, SKILL_DESCRIPTION, hitCount, critBonus);

        assertThat(skill.id()).isEqualTo(SKILL_ID);
        assertThat(skill.label()).isEqualTo(SKILL_LABEL);
        assertThat(skill.type()).isEqualTo(SkillType.HEAVY);
        assertThat(skill.talent()).isEqualTo(SkillTalent.ARCHERY);
        assertThat(skill.resourceCost()).isEqualTo(RESOURCE_COST);
        assertThat(skill.multiplierByRank()).isEqualTo(multiplierByRank);
        assertThat(skill.description()).isEqualTo(SKILL_DESCRIPTION);
        assertThat(skill.hitCount()).isEqualTo(hitCount);
        assertThat(skill.critBonus()).isEqualTo(critBonus);
    }

    /**
     * 7-인자 생성자와 hitCount=1·critBonus=0으로 만든 9-인자 생성자는 동치이다.
     */
    @Test
    @DisplayName("7-인자 생성자와 9-인자(hitCount=1, critBonus=0) 생성자는 동치")
    void should_beEqual_when_sevenArgMatchesNineArgWithDefaults() {
        final Map<SkillRank, Integer> multiplierByRank = buildMonotonicRankMap(90, 5);

        final DamageSkill sevenArg = new DamageSkill(
                SKILL_ID, SKILL_LABEL, SKILL_TYPE, SKILL_TALENT,
                RESOURCE_COST, multiplierByRank, SKILL_DESCRIPTION);

        final DamageSkill nineArg = new DamageSkill(
                SKILL_ID, SKILL_LABEL, SKILL_TYPE, SKILL_TALENT,
                RESOURCE_COST, multiplierByRank, SKILL_DESCRIPTION, 1, 0);

        assertThat(sevenArg).isEqualTo(nineArg);
        assertThat(sevenArg.hashCode()).isEqualTo(nineArg.hashCode());
    }

    /**
     * DamageSkill은 Skill 인터페이스를 구현한다.
     */
    @Test
    @DisplayName("DamageSkill은 Skill 인터페이스를 구현한다")
    void should_implementSkillInterface() {
        final Map<SkillRank, Integer> multiplierByRank = buildMonotonicRankMap(90, 5);

        final DamageSkill skill = new DamageSkill(
                SKILL_ID, SKILL_LABEL, SKILL_TYPE, SKILL_TALENT,
                RESOURCE_COST, multiplierByRank, SKILL_DESCRIPTION);

        assertThat(skill).isInstanceOf(Skill.class);
    }

    private Map<SkillRank, Integer> buildMonotonicRankMap(final int startValue,
                                                          final int increment) {
        final Map<SkillRank, Integer> rankMap = new EnumMap<>(SkillRank.class);
        int value = startValue;
        for (final SkillRank rank : SkillRank.values()) {
            rankMap.put(rank, value);
            value += increment;
        }
        return Map.copyOf(rankMap);
    }
}
