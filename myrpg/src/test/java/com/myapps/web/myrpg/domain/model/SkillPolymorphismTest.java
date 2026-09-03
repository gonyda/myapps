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

    @Test
    @DisplayName(
            "8종 스킬 서브타입의 primaryStatLabel, primaryValueAt, resourceCostAt, counter/crit 다형 계약 검증")
    void should_verifyPolymorphicValueMethodsAcrossAllSubtypes() {
        // 1. DamageSkill
        final DamageSkill damage =
                new DamageSkill(
                        "slash",
                        "베기",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        7,
                        Map.of(SkillRank.F, 90, SkillRank.E, 95),
                        "desc",
                        1,
                        15);
        assertThat(damage.primaryStatLabel()).isEqualTo("보너스 데미지");
        assertThat(damage.primaryValueAt(SkillRank.F)).isEqualTo(90);
        assertThat(damage.resourceCostAt(SkillRank.F)).isEqualTo(7);
        assertThat(damage.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(damage.critBonusValueAt(SkillRank.F)).isEmpty();

        // 2. DefenseSkill (디펜스 - 카운터 없음, 랭크별 자원소모 6)
        final DefenseSkill defense =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        5,
                        Map.of(SkillRank.F, 70),
                        Map.of(SkillRank.F, 0),
                        "desc",
                        Map.of(SkillRank.F, 6),
                        Map.of(SkillRank.F, 20));
        assertThat(defense.primaryStatLabel()).isEqualTo("피해 경감");
        assertThat(defense.primaryValueAt(SkillRank.F)).isEqualTo(70);
        assertThat(defense.resourceCostAt(SkillRank.F)).isEqualTo(6);
        assertThat(defense.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(defense.critBonusValueAt(SkillRank.F)).contains(20);

        // 3. DefenseSkill (카운터 어택 - 반격 배율 90)
        final DefenseSkill counter =
                new DefenseSkill(
                        "counter_attack",
                        "카운터 어택",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        8,
                        Map.of(SkillRank.F, 100),
                        Map.of(SkillRank.F, 90),
                        "desc");
        assertThat(counter.primaryStatLabel()).isEqualTo("피해 경감");
        assertThat(counter.primaryValueAt(SkillRank.F)).isEqualTo(100);
        assertThat(counter.resourceCostAt(SkillRank.F)).isEqualTo(8);
        assertThat(counter.counterValueAt(SkillRank.F)).contains(90);
        assertThat(counter.critBonusValueAt(SkillRank.F)).isEmpty();

        // 4. RecoverySkill
        final RecoverySkill recovery =
                new RecoverySkill(
                        "healing",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        10,
                        Map.of(SkillRank.F, 30),
                        Map.of(SkillRank.F, 12),
                        "desc");
        assertThat(recovery.primaryStatLabel()).isEqualTo("회복량");
        assertThat(recovery.primaryValueAt(SkillRank.F)).isEqualTo(30);
        assertThat(recovery.resourceCostAt(SkillRank.F)).isEqualTo(12);
        assertThat(recovery.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(recovery.critBonusValueAt(SkillRank.F)).isEmpty();

        // 5. UltimateSkill
        final UltimateSkill ultimate =
                new UltimateSkill(
                        "final_hit",
                        "파이널 히트",
                        SkillType.ULTIMATE,
                        SkillTalent.MELEE,
                        15,
                        Map.of(SkillRank.F, 250),
                        Map.of(SkillRank.F, 5),
                        100,
                        Map.of(SkillRank.F, 30),
                        "desc");
        assertThat(ultimate.primaryStatLabel()).isEqualTo("보너스 데미지");
        assertThat(ultimate.primaryValueAt(SkillRank.F)).isEqualTo(250);
        assertThat(ultimate.resourceCostAt(SkillRank.F)).isEqualTo(15);
        assertThat(ultimate.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(ultimate.critBonusValueAt(SkillRank.F)).isEmpty();

        // 6. BuffSkill
        final BuffSkill buff =
                new BuffSkill(
                        "mana_shield",
                        "마나 실드",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        8,
                        3,
                        Map.of(SkillRank.F, 20),
                        "desc");
        assertThat(buff.primaryStatLabel()).isEqualTo("흡수율");
        assertThat(buff.primaryValueAt(SkillRank.F)).isEqualTo(20);
        assertThat(buff.resourceCostAt(SkillRank.F)).isEqualTo(8);
        assertThat(buff.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(buff.critBonusValueAt(SkillRank.F)).isEmpty();

        // 7. CcSkill
        final CcSkill cc =
                new CcSkill(
                        "spider_shot",
                        "스파이더 샷",
                        SkillType.CC,
                        SkillTalent.ARCHERY,
                        9,
                        Map.of(SkillRank.F, 85),
                        "desc");
        assertThat(cc.primaryStatLabel()).isEqualTo("성공률");
        assertThat(cc.primaryValueAt(SkillRank.F)).isEqualTo(85);
        assertThat(cc.resourceCostAt(SkillRank.F)).isEqualTo(9);
        assertThat(cc.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(cc.critBonusValueAt(SkillRank.F)).isEmpty();

        // 8. DotSkill
        final DotSkill dot =
                new DotSkill(
                        "mirage_missile",
                        "미라지 미사일",
                        SkillType.DOT,
                        SkillTalent.ARCHERY,
                        10,
                        Map.of(SkillRank.F, 35),
                        Map.of(SkillRank.F, 40),
                        Map.of(SkillRank.F, 3),
                        "desc");
        assertThat(dot.primaryStatLabel()).isEqualTo("초기 배율");
        assertThat(dot.primaryValueAt(SkillRank.F)).isEqualTo(35);
        assertThat(dot.resourceCostAt(SkillRank.F)).isEqualTo(10);
        assertThat(dot.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(dot.critBonusValueAt(SkillRank.F)).isEmpty();

        // 9. PassiveSkill
        final PassiveSkill passive =
                new PassiveSkill(
                        "meditation",
                        "메디테이션",
                        SkillType.PASSIVE,
                        SkillTalent.MAGIC,
                        0,
                        Map.of(BonusTarget.MP, 30),
                        "desc");
        assertThat(passive.primaryStatLabel()).isEqualTo("패시브 효과");
        assertThat(passive.primaryValueAt(SkillRank.F)).isZero();
        assertThat(passive.resourceCostAt(SkillRank.F)).isZero();
        assertThat(passive.counterValueAt(SkillRank.F)).isEmpty();
        assertThat(passive.critBonusValueAt(SkillRank.F)).isEmpty();
    }
}
