package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link SkillDamagePolicy}의 맵 조회 정확성 단위 테스트. */
class SkillDamagePolicyTest {

    private final SkillDamagePolicy policy = new SkillDamagePolicy();

    @Test
    @DisplayName("딜스킬 multiplier: 맵에서 정확히 조회된다")
    void should_returnCorrectMultiplier_when_rankExists() {
        final Map<SkillRank, Integer> multiplierMap = createMonotonicMap(100, 10);
        final DamageSkill skill =
                new DamageSkill(
                        "smash",
                        "스매시",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        10,
                        multiplierMap,
                        "강타");

        assertThat(policy.multiplier(skill, SkillRank.F)).isEqualTo(100);
        assertThat(policy.multiplier(skill, SkillRank.A)).isEqualTo(150);
        assertThat(policy.multiplier(skill, SkillRank.MASTER)).isEqualTo(250);
    }

    @Test
    @DisplayName("디펜스 스킬 blockRate: 맵에서 정확히 조회된다")
    void should_returnCorrectBlockRate_when_rankExists() {
        final Map<SkillRank, Integer> blockRateMap = createMonotonicMap(10, 3);
        final Map<SkillRank, Integer> counterMap = createMonotonicMap(50, 5);
        final DefenseSkill skill =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        5,
                        blockRateMap,
                        counterMap,
                        "방어");

        assertThat(policy.blockRate(skill, SkillRank.F)).isEqualTo(10);
        assertThat(policy.blockRate(skill, SkillRank.MASTER)).isEqualTo(55);
    }

    @Test
    @DisplayName("디펜스 스킬 counterMultiplier: 맵에서 정확히 조회된다")
    void should_returnCorrectCounterMultiplier_when_rankExists() {
        final Map<SkillRank, Integer> blockRateMap = createMonotonicMap(10, 3);
        final Map<SkillRank, Integer> counterMap = createMonotonicMap(50, 5);
        final DefenseSkill skill =
                new DefenseSkill(
                        "defense",
                        "디펜스",
                        SkillType.DEFENSE,
                        SkillTalent.COMMON,
                        5,
                        blockRateMap,
                        counterMap,
                        "방어");

        assertThat(policy.counterMultiplier(skill, SkillRank.F)).isEqualTo(50);
        assertThat(policy.counterMultiplier(skill, SkillRank.MASTER)).isEqualTo(125);
    }

    @Test
    @DisplayName("맵에 랭크 키가 없으면 IllegalArgumentException 발생")
    void should_throwException_when_rankKeyMissing() {
        final Map<SkillRank, Integer> incompleteMap = new EnumMap<>(SkillRank.class);
        incompleteMap.put(SkillRank.F, 100);
        final DamageSkill skill =
                new DamageSkill(
                        "broken",
                        "깨진스킬",
                        SkillType.NORMAL,
                        SkillTalent.MELEE,
                        10,
                        incompleteMap,
                        "깨짐");

        assertThatThrownBy(() -> policy.multiplier(skill, SkillRank.E))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("궁극기, 힐링, 마나실드, CC, 지속피해, 빙결 수치 조회 검증")
    void should_returnCorrectValues_for_newSkillTypes() {
        // given
        final Map<SkillRank, Integer> monotonicMap = createMonotonicMap(10, 5);
        final UltimateSkill ultimate =
                new UltimateSkill(
                        "meteor",
                        "메테오",
                        SkillType.ULTIMATE,
                        SkillTalent.MAGIC,
                        40,
                        monotonicMap,
                        Map.of(SkillRank.F, 1, SkillRank.MASTER, 1),
                        10,
                        Map.of(SkillRank.F, 30, SkillRank.MASTER, 10),
                        "desc");

        final RecoverySkill recovery =
                new RecoverySkill(
                        "healing",
                        "힐링",
                        SkillType.RECOVERY,
                        SkillTalent.MAGIC,
                        12,
                        monotonicMap,
                        Map.of(SkillRank.F, 12, SkillRank.MASTER, 6),
                        "desc");

        final BuffSkill buff =
                new BuffSkill(
                        "mana_shield",
                        "마나 실드",
                        SkillType.BUFF,
                        SkillTalent.MAGIC,
                        15,
                        5,
                        monotonicMap,
                        "desc");

        final CcSkill cc =
                new CcSkill(
                        "spider_shot",
                        "스파이더 샷",
                        SkillType.CC,
                        SkillTalent.ARCHERY,
                        8,
                        monotonicMap,
                        "desc");

        final DotSkill dot =
                new DotSkill(
                        "mirage_missile",
                        "미라지 미사일",
                        SkillType.DOT,
                        SkillTalent.ARCHERY,
                        10,
                        monotonicMap,
                        monotonicMap,
                        Map.of(SkillRank.F, 1, SkillRank.MASTER, 5),
                        "desc");

        final DamageSkill freezeSkill =
                new DamageSkill(
                        "ice_spear",
                        "아이스 스피어",
                        SkillType.HEAVY,
                        SkillTalent.MAGIC,
                        20,
                        monotonicMap,
                        "desc",
                        2,
                        0,
                        0,
                        0,
                        false,
                        monotonicMap);

        // when & then
        assertThat(policy.ultimateMultiplier(ultimate, SkillRank.F)).isEqualTo(10);
        assertThat(policy.ultimateHitCount(ultimate, SkillRank.F)).isEqualTo(1);
        assertThat(policy.ultimateCoolWins(ultimate, SkillRank.F)).isEqualTo(30);

        assertThat(policy.recoveryHealAmount(recovery, SkillRank.F)).isEqualTo(10);
        assertThat(policy.recoveryCost(recovery, SkillRank.F)).isEqualTo(12);

        assertThat(policy.buffAbsorbRate(buff, SkillRank.F)).isEqualTo(10);
        assertThat(policy.ccSuccessRate(cc, SkillRank.F)).isEqualTo(10);

        assertThat(policy.dotInitialMultiplier(dot, SkillRank.F)).isEqualTo(10);
        assertThat(policy.dotDamagePerTurn(dot, SkillRank.F)).isEqualTo(10);
        assertThat(policy.dotTurns(dot, SkillRank.F)).isEqualTo(1);

        assertThat(policy.freezeRate(freezeSkill, SkillRank.F)).isEqualTo(10);
    }

    /** 16개 랭크에 대해 단조 증가하는 맵을 생성한다. */
    private Map<SkillRank, Integer> createMonotonicMap(final int base, final int increment) {
        final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            map.put(rank, base + rank.order() * increment);
        }
        return map;
    }
}
