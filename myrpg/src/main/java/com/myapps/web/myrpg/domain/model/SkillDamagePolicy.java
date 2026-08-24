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

    /**
     * 궁극기 스킬의 지정 랭크 데미지 배율(%)을 반환한다.
     *
     * @param skill 궁극기 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 데미지 배율(%)
     */
    public int ultimateMultiplier(final UltimateSkill skill, final SkillRank rank) {
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
     * 궁극기 스킬의 지정 랭크 타격 횟수를 반환한다.
     *
     * @param skill 궁극기 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 타격 횟수
     */
    public int ultimateHitCount(final UltimateSkill skill, final SkillRank rank) {
        return skill.hitCountAt(rank);
    }

    /**
     * 궁극기 스킬의 지정 랭크 쿨타임(필요 승리 횟수)을 반환한다.
     *
     * @param skill 궁극기 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 필요 승리 횟수
     */
    public int ultimateCoolWins(final UltimateSkill skill, final SkillRank rank) {
        return skill.coolWinsAt(rank);
    }

    /**
     * 힐링 스킬의 지정 랭크 HP 회복량을 반환한다.
     *
     * @param skill 힐링 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 HP 회복량
     */
    public int recoveryHealAmount(final RecoverySkill skill, final SkillRank rank) {
        return skill.healAmountAt(rank);
    }

    /**
     * 힐링 스킬의 지정 랭크 마나 소모량을 반환한다.
     *
     * @param skill 힐링 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 마나 소모량
     */
    public int recoveryCost(final RecoverySkill skill, final SkillRank rank) {
        return skill.resourceCostAt(rank);
    }

    /**
     * 마나 실드 스킬의 지정 랭크 피해 감쇄율(%)을 반환한다.
     *
     * @param skill 마나 실드 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 감쇄율(%)
     */
    public int buffAbsorbRate(final BuffSkill skill, final SkillRank rank) {
        return skill.absorbRateAt(rank);
    }

    /**
     * 군중 제어(CC) 스킬의 지정 랭크 성공률(%)을 반환한다.
     *
     * @param skill CC 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 성공률(%)
     */
    public int ccSuccessRate(final CcSkill skill, final SkillRank rank) {
        return skill.successRateAt(rank);
    }

    /**
     * 지속 피해(DOT) 스킬의 지정 랭크 즉발 배율(%)을 반환한다.
     *
     * @param skill DOT 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 즉발 배율(%)
     */
    public int dotInitialMultiplier(final DotSkill skill, final SkillRank rank) {
        return skill.initialMultiplierAt(rank);
    }

    /**
     * 지속 피해(DOT) 스킬의 지정 랭크 턴당 독 피해량을 반환한다.
     *
     * @param skill DOT 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 턴당 독 피해량
     */
    public int dotDamagePerTurn(final DotSkill skill, final SkillRank rank) {
        return skill.dotPerTurnAt(rank);
    }

    /**
     * 지속 피해(DOT) 스킬의 지정 랭크 지속 턴 수를 반환한다.
     *
     * @param skill DOT 스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 지속 턴 수
     */
    public int dotTurns(final DotSkill skill, final SkillRank rank) {
        return skill.dotTurnsAt(rank);
    }

    /**
     * 딜스킬의 지정 랭크 빙결 CC 확률(%)을 반환한다.
     *
     * @param skill 딜스킬 카탈로그 항목
     * @param rank 조회할 스킬 랭크
     * @return 해당 랭크의 빙결 확률(%)
     */
    public int freezeRate(final DamageSkill skill, final SkillRank rank) {
        return skill.freezeRateAt(rank);
    }
}
