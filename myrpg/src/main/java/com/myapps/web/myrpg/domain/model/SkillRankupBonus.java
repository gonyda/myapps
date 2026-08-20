package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 보유 스킬 목록으로부터 랭크업 영구 스탯 보너스를 합산하는 순수 계산 클래스.
 *
 * <p>규칙: 각 보유 스킬에 대해 {@code rank.order() × 재능별 주 스탯 +1}을 누적한다. 가산 대상은 스탯 계열(STR/DEX/INT/DEF)뿐이며
 * HP/MP/Stamina/Critical에는 0을 부여한다.
 *
 * <p>예시:
 *
 * <ul>
 *   <li>windmill(MELEE, rank A=order 5) → STR += 5
 *   <li>7개 스킬 전부 MASTER(order 15): MELEE×2 + ARCHERY×2 + MAGIC×2 + COMMON×1 → STR +30, DEX +30, INT
 *       +30, DEF +15
 * </ul>
 *
 * <p>이 클래스는 Spring 빈이 아닌 순수 정책 객체로 사용된다. 서비스 계층이 카탈로그 조회 함수를 주입하여 도메인↔응용 계층 의존을 차단한다.
 *
 * @see BonusTarget#STR
 * @see BonusTarget#DEX
 * @see BonusTarget#INT
 * @see BonusTarget#DEF
 */
public class SkillRankupBonus {

    /**
     * 보유 스킬 목록과 카탈로그 조회 함수로 스킬 랭크업 영구 보너스를 합산한다.
     *
     * <p>카탈로그에서 찾을 수 없는 스킬은 보너스 계산에서 제외한다.
     *
     * @param owned 캐릭터가 보유한 스킬 목록
     * @param skillLookup 스킬 ID로 카탈로그 항목을 조회하는 함수
     * @return 합산된 스탯 보너스 (스탯 계열만 양수, 나머지 0)
     */
    public Stats sum(
            final List<CharacterSkill> owned, final Function<String, Optional<Skill>> skillLookup) {
        Stats result = Stats.ZERO;
        for (final CharacterSkill characterSkill : owned) {
            final Optional<Skill> catalogEntry = skillLookup.apply(characterSkill.getSkillId());
            if (catalogEntry.isEmpty()) {
                continue;
            }
            final Skill skill = catalogEntry.get();
            final int bonus = characterSkill.getRank().order();
            if (bonus == 0) {
                continue;
            }
            result = applyBonus(result, skill.talent().rankupStatTarget(), bonus);
        }
        return result;
    }

    private Stats applyBonus(final Stats current, final BonusTarget target, final int delta) {
        return switch (target) {
            case STR -> current.withStrDelta(delta);
            case DEX -> current.withDexDelta(delta);
            case INT -> current.withIntDelta(delta);
            case DEF -> current.withDefenseDelta(delta);
            case CRITICAL, HP, MP, STAMINA -> current;
        };
    }
}
