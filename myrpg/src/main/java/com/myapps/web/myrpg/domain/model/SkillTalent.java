package com.myapps.web.myrpg.domain.model;

import java.util.Optional;

/**
 * 스킬의 재능 분류를 정의하는 열거형.
 *
 * <p>각 상수는 대응하는 {@link TalentType}(또는 {@code null}), 자원 종류({@link ResourceKind}), 랭크업 영구 보너스
 * 대상({@link BonusTarget})을 보유한다.
 *
 * <ul>
 *   <li>{@link #matchingTalent()} — 재능 매칭 판정 소스. {@code COMMON}은 매칭 없음(empty).
 *   <li>{@link #resourceKind()} — {@code MAGIC}은 MP, 그 외는 STAMINA.
 *   <li>{@link #rankupStatTarget()} — 랭크업 시 영구 가산되는 스탯 대상.
 * </ul>
 */
public enum SkillTalent {

    /** 근접전투 재능. */
    MELEE(TalentType.MELEE, ResourceKind.STAMINA, BonusTarget.STR),

    /** 활 재능. */
    ARCHERY(TalentType.ARCHERY, ResourceKind.STAMINA, BonusTarget.DEX),

    /** 마법 재능. */
    MAGIC(TalentType.MAGIC, ResourceKind.MP, BonusTarget.INT),

    /** 공용 (매칭 재능 없음). */
    COMMON(null, ResourceKind.STAMINA, BonusTarget.DEF);

    private final TalentType matchingTalent;
    private final ResourceKind resourceKind;
    private final BonusTarget rankupStatTarget;

    SkillTalent(
            final TalentType matchingTalent,
            final ResourceKind resourceKind,
            final BonusTarget rankupStatTarget) {
        this.matchingTalent = matchingTalent;
        this.resourceKind = resourceKind;
        this.rankupStatTarget = rankupStatTarget;
    }

    /**
     * 이 재능에 대응하는 {@link TalentType}을 반환한다.
     *
     * <p>{@code COMMON}은 매칭 재능이 없으므로 빈 {@code Optional}을 반환한다. 전투(7순위)에서 재능 일치 +10% 판정의 소스로 사용된다.
     *
     * @return 대응 {@code TalentType}을 담은 {@code Optional}, {@code COMMON}이면 빈 값
     */
    public Optional<TalentType> matchingTalent() {
        return Optional.ofNullable(matchingTalent);
    }

    /**
     * 이 재능 분류의 스킬이 소모하는 자원 종류를 반환한다.
     *
     * <p>{@code MAGIC}은 {@link ResourceKind#MP}, 그 외({@code MELEE}/{@code ARCHERY}/{@code COMMON})는
     * {@link ResourceKind#STAMINA}를 반환한다.
     *
     * @return 자원 종류 {@link ResourceKind}
     */
    public ResourceKind resourceKind() {
        return resourceKind;
    }

    /**
     * 랭크업 시 영구 가산되는 스탯 대상을 반환한다.
     *
     * <p>{@code MELEE}→{@link BonusTarget#STR}, {@code ARCHERY}→{@link BonusTarget#DEX}, {@code
     * MAGIC}→{@link BonusTarget#INT}, {@code COMMON}→{@link BonusTarget#DEF}.
     *
     * @return 랭크업 보너스 대상 {@link BonusTarget}
     */
    public BonusTarget rankupStatTarget() {
        return rankupStatTarget;
    }

    /**
     * 문자열로부터 {@code SkillTalent}을 안전하게 조회한다.
     *
     * <p>상수명 비교는 대소문자를 구분한다(예: "MELEE"). {@code null}, 공백, 알려지지 않은 문자열이면 빈 {@code Optional}을 반환한다.
     *
     * @param value 조회할 스킬 재능 상수명
     * @return 유효한 상수명이면 해당 {@code SkillTalent}을 담은 {@code Optional}, 그 외 빈 값
     */
    public static Optional<SkillTalent> fromString(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SkillTalent.valueOf(value));
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
