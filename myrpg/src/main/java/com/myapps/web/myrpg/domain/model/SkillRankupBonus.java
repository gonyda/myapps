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
     * <p>카탈로그에서 찾을 수 없는 스킬이나 F 랭크(order 0)는 보너스 계산에서 제외한다. 카운터 어택은 영구 스탯을 제공하지 않는다. 패시브 스킬 6종은
     * MASTER 기준 스탯에 대해 {@code rank.order() / 15.0} 선형 비율로 반올림 누적한다.
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
            if (catalogEntry.isEmpty() || characterSkill.getRank().order() == 0) {
                continue;
            }
            result =
                    accumulateSkillStats(
                            result, catalogEntry.get(), characterSkill.getRank().order());
        }
        return result;
    }

    private Stats accumulateSkillStats(final Stats current, final Skill skill, final int order) {
        if (skill instanceof PassiveSkill ps) {
            return accumulatePassiveStats(current, ps, order);
        }
        if ("defense".equals(skill.id())) {
            return current.withDefenseDelta(order);
        }
        if ("counter_attack".equals(skill.id())) {
            return current;
        }
        return applyBonus(current, skill.talent().rankupStatTarget(), order);
    }

    private Stats accumulatePassiveStats(
            final Stats current, final PassiveSkill ps, final int order) {
        if (ps.totalStatBonus() == null) {
            return current;
        }
        Stats result = current;
        for (final var entry : ps.totalStatBonus().entrySet()) {
            final int rankBonus = Math.round((float) entry.getValue() * order / 15.0f);
            result = applyBonus(result, entry.getKey(), rankBonus);
        }
        return result;
    }

    /**
     * 보유 스킬 목록과 카탈로그 조회 함수로 스킬 랭크업 영구 바이탈(HP/MP/Stamina) 보너스를 합산한다.
     *
     * <p>디펜스({@code defense})는 랭크당 HP +5 누적 보너스를 부여하며, 패시브 스킬은 {@code totalStatBonus}의 바이탈 항목을 선형
     * 누적한다.
     *
     * @param owned 캐릭터가 보유한 스킬 목록
     * @param skillLookup 스킬 ID로 카탈로그 항목을 조회하는 함수
     * @return 합산된 바이탈 보너스 (HP, MP, Stamina)
     */
    public VitalMax sumVital(
            final List<CharacterSkill> owned, final Function<String, Optional<Skill>> skillLookup) {
        VitalAccumulator acc = new VitalAccumulator(0, 0, 0);
        for (final CharacterSkill characterSkill : owned) {
            final Optional<Skill> catalogEntry = skillLookup.apply(characterSkill.getSkillId());
            if (catalogEntry.isEmpty() || characterSkill.getRank().order() == 0) {
                continue;
            }
            acc = accumulateSkillVital(acc, catalogEntry.get(), characterSkill.getRank().order());
        }
        return new VitalMax(acc.hp(), acc.mp(), acc.stamina());
    }

    private VitalAccumulator accumulateSkillVital(
            final VitalAccumulator acc, final Skill skill, final int order) {
        if (skill instanceof PassiveSkill ps) {
            return accumulatePassiveVital(acc, ps, order);
        }
        if ("defense".equals(skill.id())) {
            return acc.addHp(order * 5);
        }
        return acc;
    }

    private VitalAccumulator accumulatePassiveVital(
            final VitalAccumulator acc, final PassiveSkill ps, final int order) {
        if (ps.totalStatBonus() == null) {
            return acc;
        }
        VitalAccumulator current = acc;
        for (final var entry : ps.totalStatBonus().entrySet()) {
            final int rankBonus = Math.round((float) entry.getValue() * order / 15.0f);
            current =
                    switch (entry.getKey()) {
                        case HP -> current.addHp(rankBonus);
                        case MP -> current.addMp(rankBonus);
                        case STAMINA -> current.addStamina(rankBonus);
                        default -> current;
                    };
        }
        return current;
    }

    private record VitalAccumulator(int hp, int mp, int stamina) {
        VitalAccumulator addHp(final int delta) {
            return new VitalAccumulator(hp + delta, mp, stamina);
        }

        VitalAccumulator addMp(final int delta) {
            return new VitalAccumulator(hp, mp + delta, stamina);
        }

        VitalAccumulator addStamina(final int delta) {
            return new VitalAccumulator(hp, mp, stamina + delta);
        }
    }

    /**
     * 보유 스킬 목록에서 메디테이션 랭크에 따른 전투 턴 종료 턴당 MP 회복량을 계산한다.
     *
     * <p>F~D: 1, C~A: 2, 9~7: 3, 6~4: 4, 3~Master: 5 MP를 회복한다.
     *
     * @param owned 캐릭터가 보유한 스킬 목록
     * @return 턴당 MP 회복량 (메디테이션 미보유 시 0)
     */
    public int sumMpRegen(
            final List<CharacterSkill> owned, final Function<String, Optional<Skill>> skillLookup) {
        for (final CharacterSkill characterSkill : owned) {
            if ("meditation".equals(characterSkill.getSkillId())) {
                final int order = characterSkill.getRank().order();
                return Math.min(5, (order / 3) + 1);
            }
        }
        return 0;
    }

    private Stats applyBonus(final Stats current, final BonusTarget target, final int delta) {
        return switch (target) {
            case STR -> current.withStrDelta(delta);
            case DEX -> current.withDexDelta(delta);
            case INT -> current.withIntDelta(delta);
            case DEF -> current.withDefenseDelta(delta);
            case CRITICAL -> current.withCriticalDelta(delta);
            case HP, MP, STAMINA, MP_REGEN -> current;
        };
    }
}
