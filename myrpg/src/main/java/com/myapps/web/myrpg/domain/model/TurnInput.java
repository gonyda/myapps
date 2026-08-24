package com.myapps.web.myrpg.domain.model;

/**
 * 전투 턴 해결에 필요한 입력 값을 담는 레코드.
 *
 * <p>{@link com.myapps.web.myrpg.domain.service.BattleResolver#resolve(TurnInput)}에 전달되어 9칸 매트릭스 기반
 * 양측 피해를 산출하는 데 사용된다. 선후공 로직은 포함하지 않으며, 순수 데미지 계산에 필요한 수치만 집약한다.
 *
 * @param playerType 플레이어 스킬 타입
 * @param monsterType 몬스터 스킬 타입
 * @param playerAttackPower 플레이어 공격력 (주스탯 × 재능계수 결과)
 * @param monsterAttackPower 몬스터 공격력
 * @param playerDefense 플레이어 방어력
 * @param monsterDefense 몬스터 방어력
 * @param playerMultiplierPercent 플레이어 스킬 1히트당 배율(%)
 * @param monsterMultiplierPercent 몬스터 스킬 배율(%) — 일반 100 / 강 150
 * @param playerBlockRatePercent 플레이어 방어 경감률(%)
 * @param monsterBlockRatePercent 몬스터 방어 경감률(%) — 기본 100
 * @param playerCounterPercent 플레이어 반격 배율(%) — 반격 시 공격력의 N%를 피해로 적용
 * @param monsterCounterPercent 몬스터 반격률(%) — 기본 0
 * @param playerCritical 플레이어 크리티컬 수치(0.1% 단위, 범위 0~1000)
 * @param monsterCritical 몬스터 크리티컬 수치(0.1% 단위, 범위 0~1000)
 * @param playerHitCount 플레이어 히트 수 (딜 스킬의 hitCount, 기본 1)
 * @param isCounterAttack 카운터 어택 스킬 여부 (상대 공격력 비례 반격 및 강공격 반격 지원)
 * @param playerDefensePierce 플레이어 방어 관통 여부 (적 DEF 0 처리)
 */
public record TurnInput(
        SkillType playerType,
        SkillType monsterType,
        int playerAttackPower,
        int monsterAttackPower,
        int playerDefense,
        int monsterDefense,
        int playerMultiplierPercent,
        int monsterMultiplierPercent,
        int playerBlockRatePercent,
        int monsterBlockRatePercent,
        int playerCounterPercent,
        int monsterCounterPercent,
        int playerCritical,
        int monsterCritical,
        int playerHitCount,
        boolean isCounterAttack,
        boolean playerDefensePierce) {

    /**
     * 하위 호환 16-인자 보조 생성자 (playerDefensePierce = false).
     *
     * @param playerType 플레이어 스킬 타입
     * @param monsterType 몬스터 스킬 타입
     * @param playerAttackPower 플레이어 공격력
     * @param monsterAttackPower 몬스터 공격력
     * @param playerDefense 플레이어 방어력
     * @param monsterDefense 몬스터 방어력
     * @param playerMultiplierPercent 플레이어 스킬 1히트당 배율(%)
     * @param monsterMultiplierPercent 몬스터 스킬 배율(%)
     * @param playerBlockRatePercent 플레이어 방어 경감률(%)
     * @param monsterBlockRatePercent 몬스터 방어 경감률(%)
     * @param playerCounterPercent 플레이어 반격 배율(%)
     * @param monsterCounterPercent 몬스터 반격률(%)
     * @param playerCritical 플레이어 크리티컬 수치
     * @param monsterCritical 몬스터 크리티컬 수치
     * @param playerHitCount 플레이어 히트 수
     * @param isCounterAttack 카운터 어택 여부
     */
    public TurnInput(
            final SkillType playerType,
            final SkillType monsterType,
            final int playerAttackPower,
            final int monsterAttackPower,
            final int playerDefense,
            final int monsterDefense,
            final int playerMultiplierPercent,
            final int monsterMultiplierPercent,
            final int playerBlockRatePercent,
            final int monsterBlockRatePercent,
            final int playerCounterPercent,
            final int monsterCounterPercent,
            final int playerCritical,
            final int monsterCritical,
            final int playerHitCount,
            final boolean isCounterAttack) {
        this(
                playerType,
                monsterType,
                playerAttackPower,
                monsterAttackPower,
                playerDefense,
                monsterDefense,
                playerMultiplierPercent,
                monsterMultiplierPercent,
                playerBlockRatePercent,
                monsterBlockRatePercent,
                playerCounterPercent,
                monsterCounterPercent,
                playerCritical,
                monsterCritical,
                playerHitCount,
                isCounterAttack,
                false);
    }

    /**
     * 하위 호환 15-인자 보조 생성자 (isCounterAttack = false, playerDefensePierce = false).
     *
     * @param playerType 플레이어 스킬 타입
     * @param monsterType 몬스터 스킬 타입
     * @param playerAttackPower 플레이어 공격력
     * @param monsterAttackPower 몬스터 공격력
     * @param playerDefense 플레이어 방어력
     * @param monsterDefense 몬스터 방어력
     * @param playerMultiplierPercent 플레이어 스킬 1히트당 배율(%)
     * @param monsterMultiplierPercent 몬스터 스킬 배율(%)
     * @param playerBlockRatePercent 플레이어 방어 경감률(%)
     * @param monsterBlockRatePercent 몬스터 방어 경감률(%)
     * @param playerCounterPercent 플레이어 반격 배율(%)
     * @param monsterCounterPercent 몬스터 반격률(%)
     * @param playerCritical 플레이어 크리티컬 수치
     * @param monsterCritical 몬스터 크리티컬 수치
     * @param playerHitCount 플레이어 히트 수
     */
    public TurnInput(
            final SkillType playerType,
            final SkillType monsterType,
            final int playerAttackPower,
            final int monsterAttackPower,
            final int playerDefense,
            final int monsterDefense,
            final int playerMultiplierPercent,
            final int monsterMultiplierPercent,
            final int playerBlockRatePercent,
            final int monsterBlockRatePercent,
            final int playerCounterPercent,
            final int monsterCounterPercent,
            final int playerCritical,
            final int monsterCritical,
            final int playerHitCount) {
        this(
                playerType,
                monsterType,
                playerAttackPower,
                monsterAttackPower,
                playerDefense,
                monsterDefense,
                playerMultiplierPercent,
                monsterMultiplierPercent,
                playerBlockRatePercent,
                monsterBlockRatePercent,
                playerCounterPercent,
                monsterCounterPercent,
                playerCritical,
                monsterCritical,
                playerHitCount,
                false,
                false);
    }
}
