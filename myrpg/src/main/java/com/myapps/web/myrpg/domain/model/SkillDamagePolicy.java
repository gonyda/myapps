package com.myapps.web.myrpg.domain.model;

/**
 * 스킬·랭크 조합에 대한 수치를 맵에서 조회하는 순수 정책 클래스.
 *
 * <p>딜스킬(NORMAL/HEAVY)은 랭크별 데미지 배율(%), 디펜스 스킬은 랭크별 피해 경감률(%)과 반격 배율(%)을 제공한다. 보간 없이 카탈로그 맵에서 직접
 * 조회한다.
 *
 * <p>전투(6순위)가 실제 데미지·방어 계산에서 소비하며, 본 스펙(005)은 조회 정책과 승급 모달 표시용까지만 구현한다.
 *
 * @see DamageSkill#multiplierByRank()
 * @see DefenseSkill#blockRateByRank()
 * @see DefenseSkill#counterMultiplierByRank()
 */
public class SkillDamagePolicy {

    /**
     * 딜스킬의 지정 랭크 데미지 배율(%)을 반환한다.
     *
     * @param skill 딜스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 데미지 배율(%)
     * @throws IllegalArgumentException 맵에 해당 랭크 키가 없을 경우
     */
    public int multiplier(final DamageSkill skill, final SkillRank rank) {
        final Integer value = skill.multiplierByRank().get(rank);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Rank "
                            + rank.label()
                            + " not found in multiplierByRank for skill: "
                            + skill.id());
        }
        return value;
    }

    /**
     * 디펜스 스킬의 지정 랭크 피해 경감률(%)을 반환한다.
     *
     * @param skill 디펜스 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 피해 경감률(%)
     * @throws IllegalArgumentException 맵에 해당 랭크 키가 없을 경우
     */
    public int blockRate(final DefenseSkill skill, final SkillRank rank) {
        final Integer value = skill.blockRateByRank().get(rank);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Rank "
                            + rank.label()
                            + " not found in blockRateByRank for skill: "
                            + skill.id());
        }
        return value;
    }

    /**
     * 디펜스 스킬의 지정 랭크 반격 배율(%)을 반환한다.
     *
     * @param skill 디펜스 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 반격 배율(%)
     * @throws IllegalArgumentException 맵에 해당 랭크 키가 없을 경우
     */
    public int counterMultiplier(final DefenseSkill skill, final SkillRank rank) {
        final Integer value = skill.counterMultiplierByRank().get(rank);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Rank "
                            + rank.label()
                            + " not found in counterMultiplierByRank for skill: "
                            + skill.id());
        }
        return value;
    }
}
