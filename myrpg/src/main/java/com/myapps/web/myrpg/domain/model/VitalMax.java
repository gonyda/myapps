package com.myapps.web.myrpg.domain.model;

/**
 * HP, MP, Stamina 각각의 최대치를 나타내는 순수 표시 VO (Value Object).
 *
 * <p>이 record는 JPA 엔티티에 직접 매핑되지 않으며, 레벨·재능 등으로부터 계산되어 조립된 바이탈별 최대치를 표현한다. 단일 정수 최대치를 대체하여 재능이 특정
 * 바이탈만 성장시킬 수 있도록 한다. 델타 헬퍼는 대상 필드만 변경한 새 인스턴스를 반환한다.
 *
 * @param hp 체력 최대치
 * @param mp 마나 최대치
 * @param stamina 기력 최대치
 */
public record VitalMax(int hp, int mp, int stamina) {

    /**
     * HP 최대치에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta HP 최대치에 가산할 값 (음수 가능)
     * @return HP만 변경된 새 VitalMax 인스턴스
     */
    public VitalMax withHpDelta(final int delta) {
        return new VitalMax(hp + delta, mp, stamina);
    }

    /**
     * MP 최대치에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta MP 최대치에 가산할 값 (음수 가능)
     * @return MP만 변경된 새 VitalMax 인스턴스
     */
    public VitalMax withMpDelta(final int delta) {
        return new VitalMax(hp, mp + delta, stamina);
    }

    /**
     * Stamina 최대치에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta Stamina 최대치에 가산할 값 (음수 가능)
     * @return Stamina만 변경된 새 VitalMax 인스턴스
     */
    public VitalMax withStaminaDelta(final int delta) {
        return new VitalMax(hp, mp, stamina + delta);
    }
}
