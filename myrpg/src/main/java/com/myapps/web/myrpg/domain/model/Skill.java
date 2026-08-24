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
}
