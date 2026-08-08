package com.myapps.web.myrpg.domain.model;

/**
 * 재능의 성장 효과를 (대상, 레벨당 증가치) 쌍으로 표현하는 순수 VO (Value Object).
 *
 * <p>재능의 주 성장·보조 성장을 하나의 어휘로 표현하며 JPA 엔티티에 직접 매핑되지 않는다.
 * {@code CRITICAL} 대상의 {@code perLevel}은 0.1% 단위 정수(값 {@code 1} = +0.1%/Lv)이며,
 * 바이탈 계열({@code HP}/{@code MP}/{@code STAMINA}) 대상의 {@code perLevel}은 최대치 증가량을 의미한다.
 *
 * @param target   보너스가 적용되는 대상
 * @param perLevel 레벨당 증가치 (대상 종류에 따라 단위 상이)
 */
public record TalentBonus(
        BonusTarget target,
        int perLevel
) {
}
