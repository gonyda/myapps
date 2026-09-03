package com.myapps.web.myrpg.domain.model;

/**
 * 스킬 카탈로그 항목의 공통 계약을 정의하는 sealed interface.
 *
 * <p>모든 스킬은 고유 식별자({@link #id()}), 표시 라벨({@link #label()}), 공격/방어 유형({@link #type()}), 재능 분류({@link
 * #talent()}), 자원 소모량({@link #resourceCost()}), 스킬 설명({@link #description()})을 가진다.
 *
 * <p>구현체는 8종 도메인 record({@link DamageSkill}, {@link DefenseSkill}, {@link RecoverySkill}, {@link
 * UltimateSkill}, {@link PassiveSkill}, {@link BuffSkill}, {@link CcSkill}, {@link DotSkill})로
 * 제한된다.
 *
 * @see DamageSkill
 * @see DefenseSkill
 * @see RecoverySkill
 * @see UltimateSkill
 * @see PassiveSkill
 * @see BuffSkill
 * @see CcSkill
 * @see DotSkill
 */
public sealed interface Skill
        permits DamageSkill,
                DefenseSkill,
                RecoverySkill,
                UltimateSkill,
                PassiveSkill,
                BuffSkill,
                CcSkill,
                DotSkill {

    /**
     * 스킬의 고유 식별자를 반환한다.
     *
     * @return 스킬 ID 문자열 (예: "windmill", "firebolt")
     */
    String id();

    /**
     * 스킬의 표시용 라벨을 반환한다.
     *
     * @return 스킬 라벨 문자열 (예: "윈드밀", "파이어볼트")
     */
    String label();

    /**
     * 스킬의 공격/방어 유형을 반환한다.
     *
     * @return 스킬 타입 {@link SkillType}
     */
    SkillType type();

    /**
     * 스킬의 재능 분류를 반환한다.
     *
     * @return 스킬 재능 {@link SkillTalent}
     */
    SkillTalent talent();

    /**
     * 스킬 사용 시 소모되는 자원량을 반환한다.
     *
     * <p>자원의 종류는 {@link SkillTalent}에서 파생되며, 이 값은 랭크와 무관하게 고정이다.
     *
     * @return 자원 소모량 (양의 정수)
     */
    int resourceCost();

    /**
     * 스킬에 대한 설명 문자열을 반환한다.
     *
     * <p>스킬의 특성과 활용법을 간략히 서술한다.
     *
     * @return 스킬 설명 문자열
     */
    String description();

    /**
     * 현재 랭크 및 다음 랭크 기준의 효과 상세 행 목록을 반환한다.
     *
     * @param currentRank 현재 랭크
     * @param nextRank 다음 랭크 (MASTER 시 null)
     * @return 효과 항목 목록
     */
    java.util.List<SkillEffectRowView> effectRowsAt(SkillRank currentRank, SkillRank nextRank);

    /**
     * 현재 랭크에서 다음 랭크로 승급 시 획득하는 실제 영구 보너스 변동량(Delta)을 반환한다.
     *
     * @param currentRank 현재 랭크
     * @param nextRank 다음 랭크 (MASTER 시 null)
     * @return 승급 스탯 델타
     */
    SkillRankupBonusDelta rankupBonusDelta(SkillRank currentRank, SkillRank nextRank);

    /**
     * 상시 영구 패시브 스킬인지 여부를 반환한다.
     *
     * @return 패시브 여부
     */
    default boolean isPassive() {
        return type() == SkillType.PASSIVE;
    }

    /**
     * 스킬 표시용 기본 스탯 라벨을 반환한다 (예: "보너스 데미지", "피해 경감", "회복량", "흡수율").
     *
     * @return 기본 스탯 라벨
     */
    String primaryStatLabel();

    /**
     * 지정 랭크에서의 기본 대표값을 반환한다 (배율/블록률/회복량/성공률 등).
     *
     * @param rank 스킬 랭크
     * @return 대표값 (정의 없으면 0)
     */
    int primaryValueAt(SkillRank rank);

    /**
     * 지정 랭크에서의 자원 소모량을 반환한다. 기본은 랭크 무관 고정값.
     *
     * @param rank 스킬 랭크
     * @return 자원 소모량
     */
    default int resourceCostAt(final SkillRank rank) {
        return resourceCost();
    }

    /**
     * 지정 랭크에서의 카운터 값을 반환한다. 카운터가 없으면 빈 값.
     *
     * @param rank 스킬 랭크
     * @return 카운터 배율 Optional
     */
    default java.util.Optional<Integer> counterValueAt(final SkillRank rank) {
        return java.util.Optional.empty();
    }

    /**
     * 지정 랭크에서의 크리티컬 보너스 값을 반환한다. 없으면 빈 값.
     *
     * @param rank 스킬 랭크
     * @return 크리 보너스 Optional
     */
    default java.util.Optional<Integer> critBonusValueAt(final SkillRank rank) {
        return java.util.Optional.empty();
    }

    /**
     * 전투 처리 방문자를 수락하여 서브타입별 처리로 디스패치한다.
     *
     * @param visitor 전투 방문자
     * @param <R> 방문 결과 타입
     * @return 방문 결과
     */
    <R> R accept(com.myapps.web.myrpg.domain.service.SkillCombatVisitor<R> visitor);
}
