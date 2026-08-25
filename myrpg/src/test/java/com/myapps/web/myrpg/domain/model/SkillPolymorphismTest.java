package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 8종 스킬 도메인 모델의 다형적 효과 및 승급 스탯 델타 산출 단위 테스트. */
class SkillPolymorphismTest {

    @Test
    @DisplayName("DamageSkill effectRowsAt 및 rankupBonusDelta 정상 산출")
    void should_generateDamageSkillEffectsAndBonusDelta() {
        final Map<SkillRank, Integer> multMap = Map.of(SkillRank.F, 90, SkillRank.E, 95);
        final DamageSkill slash =
                new DamageSkill(
                        "slash",
                        "베기",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        7,
                        multMap,
                        "desc",
                        1,
                        0);

        final List<SkillEffectRowView> rows = slash.effectRowsAt(SkillRank.F, SkillRank.E);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).label()).isEqualTo("1히트당 피해");
        assertThat(rows.get(0).currentValue()).isEqualTo("90%");
        assertThat(rows.get(0).nextValue()).isEqualTo("95%");

        final SkillRankupBonusDelta delta = slash.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(delta.strDelta()).isEqualTo(1);
        assertThat(delta.toDisplayText()).isEqualTo("체력(STR) +1");
    }

    @Test
    @DisplayName("DefenseSkill (defense) 승급 시 생명력 +5, 방어력 +1 델타 산출 및 막타 면제")
    void should_generateDefenseSkillEffectsAndBonusDelta() {
        final Map<SkillRank, Integer> blockMap = Map.of(SkillRank.F, 70, SkillRank.E, 72);
        final Map<SkillRank, Integer> counterMap = Map.of(SkillRank.F, 0, SkillRank.E, 0);
        final DefenseSkill defense =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        5,
                        blockMap,
                        counterMap,
                        "desc");

        final List<SkillEffectRowView> rows = defense.effectRowsAt(SkillRank.F, SkillRank.E);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).label()).isEqualTo("피해 경감률");
        assertThat(rows.get(0).currentValue()).isEqualTo("70%");
        assertThat(rows.get(0).nextValue()).isEqualTo("72%");

        final SkillRankupBonusDelta delta = defense.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(delta.defDelta()).isEqualTo(1);
        assertThat(delta.hpDelta()).isEqualTo(5);
        assertThat(delta.toDisplayText()).isEqualTo("방어력(DEF) +1, 최대 HP +5");

        assertThat(defense.isKillExempt()).isTrue();
        assertThat(defense.isPassive()).isFalse();
    }

    @Test
    @DisplayName("CounterAttack은 승급 스탯 보너스가 없다")
    void should_returnEmptyBonusDelta_when_counterAttack() {
        final Map<SkillRank, Integer> blockMap = Map.of(SkillRank.F, 100, SkillRank.E, 100);
        final Map<SkillRank, Integer> counterMap = Map.of(SkillRank.F, 90, SkillRank.E, 94);
        final DefenseSkill counter =
                new DefenseSkill(
                        "counter_attack",
                        "카운터 어택",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        8,
                        blockMap,
                        counterMap,
                        "desc");

        final SkillRankupBonusDelta delta = counter.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(delta.isEmpty()).isTrue();
        assertThat(delta.toDisplayText()).isNull();
    }

    @Test
    @DisplayName("Meditation (메디테이션) F->E 승급 시 최대 MP +2 산출 (INT 0)")
    void should_generateMeditationBonusDelta_withoutIntBonus() {
        final PassiveSkill meditation =
                new PassiveSkill(
                        "meditation",
                        "메디테이션",
                        SkillType.PASSIVE,
                        SkillTalent.MAGIC,
                        0,
                        Map.of(BonusTarget.MP, 30, BonusTarget.MP_REGEN, 5),
                        "desc");

        // F (order 0) -> E (order 1)
        // MP: round(30*1/15) - round(30*0/15) = 2 - 0 = 2
        // REGEN: min(5, (1/3)+1) - min(5, (0/3)+1) = 1 - 1 = 0
        final SkillRankupBonusDelta fToEDelta =
                meditation.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(fToEDelta.mpDelta()).isEqualTo(2);
        assertThat(fToEDelta.intDelta()).isZero();
        assertThat(fToEDelta.mpRegenDelta()).isZero();
        assertThat(fToEDelta.toDisplayText()).isEqualTo("최대 MP +2");

        // D (order 2) -> C (order 3)
        // REGEN: min(5, (3/3)+1) - min(5, (2/3)+1) = 2 - 1 = 1
        final SkillRankupBonusDelta dToCDelta =
                meditation.rankupBonusDelta(SkillRank.D, SkillRank.C);
        assertThat(dToCDelta.mpDelta()).isEqualTo(2);
        assertThat(dToCDelta.mpRegenDelta()).isEqualTo(1);
        assertThat(dToCDelta.toDisplayText()).isEqualTo("최대 MP +2, 턴당 MP 회복 +1");

        assertThat(meditation.isPassive()).isTrue();
        assertThat(meditation.isKillExempt()).isTrue();
    }

    @Test
    @DisplayName("RecoverySkill (힐링) effectRowsAt 및 INT +1 델타 산출")
    void should_generateRecoverySkillEffectsAndBonusDelta() {
        final RecoverySkill healing =
                new RecoverySkill(
                        "healing",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 30, SkillRank.E, 35),
                        Map.of(SkillRank.F, 12, SkillRank.E, 12),
                        "desc");

        final List<SkillEffectRowView> rows = healing.effectRowsAt(SkillRank.F, SkillRank.E);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).label()).isEqualTo("생명력 회복량");
        assertThat(rows.get(0).currentValue()).isEqualTo("30 HP");
        assertThat(rows.get(0).nextValue()).isEqualTo("35 HP");
        assertThat(rows.get(1).label()).isEqualTo("소모 마나");
        assertThat(rows.get(1).currentValue()).isEqualTo("12 MP");
        assertThat(rows.get(1).nextValue()).isEqualTo("12 MP");

        final SkillRankupBonusDelta delta = healing.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(delta.intDelta()).isEqualTo(1);
        assertThat(delta.toDisplayText()).isEqualTo("지력(INT) +1");
    }

    @Test
    @DisplayName("DotSkill (미라지 미사일) effectRowsAt 3종 및 DEX +1 델타 산출")
    void should_generateDotSkillEffectsAndBonusDelta() {
        final DotSkill dot =
                new DotSkill(
                        "mirage_missile",
                        "미라지 미사일",
                        SkillType.DOT,
                        SkillTalent.ARCHERY,
                        10,
                        Map.of(SkillRank.F, 30, SkillRank.E, 30),
                        Map.of(SkillRank.F, 40, SkillRank.E, 45),
                        Map.of(SkillRank.F, 1, SkillRank.E, 1),
                        "desc");

        final List<SkillEffectRowView> rows = dot.effectRowsAt(SkillRank.F, SkillRank.E);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).label()).isEqualTo("초기 직격 피해");
        assertThat(rows.get(1).label()).isEqualTo("독 지속 시간");
        assertThat(rows.get(2).label()).isEqualTo("턴당 독 피해");

        final SkillRankupBonusDelta delta = dot.rankupBonusDelta(SkillRank.F, SkillRank.E);
        assertThat(delta.dexDelta()).isEqualTo(1);
        assertThat(delta.toDisplayText()).isEqualTo("솜씨(DEX) +1");
    }
}
