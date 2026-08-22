package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 보유 스킬 목록으로부터 랭크업 영구 스탯 및 바이탈 보너스를 합산하는 순수 계산 클래스.
 *
 * <p>규칙:
 *
 * <ul>
 *   <li>디펜스({@code defense}): 랭크당 방어력 DEF +1, 생명력 HP +5 누적 가산 (MASTER 시 DEF +15, HP +75)
 *   <li>카운터 어택({@code counter_attack}): 영구 스탯 보너스 없음 (0)
 *   <li>기타 스킬: 각 보유 스킬에 대해 {@code rank.order() × 재능별 주 스탯 +1} 누적 (STR/DEX/INT)
 * </ul>
 *
 * <p>이 클래스는 Spring 빈이 아닌 순수 정책 객체로 사용된다. 서비스 계층이 카탈로그 조회 함수를 주입하여 도메인↔응용 계층 의존을 차단한다.
 *
 * @see BonusTarget#STR
 * @see BonusTarget#DEX
 * @see BonusTarget#INT
 * @see BonusTarget#DEF
 * @see BonusTarget#HP
 */
public class SkillRankupBonus {

    /**
     * 보유 스킬 목록과 카탈로그 조회 함수로 스킬 랭크업 영구 스탯 보너스를 합산한다.
     *
     * <p>카탈로그에서 찾을 수 없는 스킬이나 F 랭크(order 0)는 보너스 계산에서 제외한다. 카운터 어택은 영구 스탯을 제공하지 않는다.
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
            if ("defense".equals(skill.id())) {
                result = result.withDefenseDelta(bonus);
            } else if ("counter_attack".equals(skill.id())) {
                // counter_attack은 영구 스탯 보너스 없음
                continue;
            } else {
                result = applyBonus(result, skill.talent().rankupStatTarget(), bonus);
            }
        }
        return result;
    }

    /**
     * 보유 스킬 목록과 카탈로그 조회 함수로 스킬 랭크업 영구 바이탈(HP) 보너스를 합산한다.
     *
     * <p>현재 디펜스({@code defense}) 스킬만 랭크당 HP +5 누적 보너스를 부여하며, 나머지 스킬은 0이다.
     *
     * @param owned 캐릭터가 보유한 스킬 목록
     * @param skillLookup 스킬 ID로 카탈로그 항목을 조회하는 함수
     * @return 합산된 바이탈 보너스 (HP만 양수 가능, MP/Stamina는 0)
     */
    public VitalMax sumVital(
            final List<CharacterSkill> owned, final Function<String, Optional<Skill>> skillLookup) {
        int hpBonus = 0;
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
            if ("defense".equals(skill.id())) {
                hpBonus += bonus * 5;
            }
        }
        return new VitalMax(hpBonus, 0, 0);
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
