package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BattleLogFormatter}의 전투 로그 문자열 생성을 검증하는 단위 테스트.
 *
 * <p>가위바위보 9칸 매트릭스의 각 결과와 특수 턴(활 1턴 선제 사격, 마법 캐스팅 실패)에 대해 플레이어·몬스터 로그 문구가 정확히 생성되는지 확인한다. 방어 승리 상황은
 * 플레이어·몬스터 어느 쪽이 방어하든 "방어하며 반격!"으로 대칭 표현되는지 검증한다. 멀티히트(hitCount ≥ 2) 시 헤더+브레이크다운 형식이 올바르게 출력되는지
 * 검증한다.
 */
class BattleLogFormatterTest {

    private static final String MONSTER_NAME = "너구리";
    private static final String ATTACK_SKILL = "윈드밀";
    private static final String HEAVY_SKILL = "스매시";
    private static final String DEFENSE_SKILL = "디펜스";
    private static final String MAGIC_SKILL = "파이어볼트";

    private final BattleLogFormatter formatter = new BattleLogFormatter();

    @Test
    @DisplayName("일반 공격 적중 시 플레이어 피해 + 몬스터 공격 로그를 남긴다")
    void should_logHitAndMonsterAttack_when_normalAttackLands() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        10,
                        13,
                        false,
                        false,
                        false,
                        List.of(new HitResult(10, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [윈드밀] ➔ 너구리에게 10 피해", "[너구리] 일반공격 ➔ 13 피해 피격");
    }

    @Test
    @DisplayName("크리티컬 시 플레이어 피해 로그에 💥를 붙인다")
    void should_appendCriticalMark_when_playerCritical() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.HEAVY,
                        15,
                        0,
                        true,
                        false,
                        false,
                        List.of(new HitResult(15, true)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [윈드밀] 💥 ➔ 너구리에게 15 피해", "[너구리] 강공격 ➔ 빗나감");
    }

    @Test
    @DisplayName("내 강공격이 상성에 지면 빗나감 로그를 남긴다")
    void should_logPlayerMiss_when_playerAttackDealsZero() {
        final BattleLogInput input =
                new BattleLogInput(
                        HEAVY_SKILL,
                        SkillType.HEAVY,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        13,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [스매시] 빗나감 (0 피해)", "[너구리] 일반공격 ➔ 13 피해 피격");
    }

    @Test
    @DisplayName("몬스터 강공격이 상성에 지면 빗나감 로그를 남긴다")
    void should_logMonsterMiss_when_monsterAttackDealsZero() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.HEAVY,
                        10,
                        0,
                        false,
                        false,
                        false,
                        List.of(new HitResult(10, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [윈드밀] ➔ 너구리에게 10 피해", "[너구리] 강공격 ➔ 빗나감");
    }

    @Test
    @DisplayName("몬스터 방어 승리 시 경감 피해 + 몬스터 반격 로그를 남긴다")
    void should_logMonsterCounter_when_monsterDefenseWins() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        3,
                        7,
                        false,
                        false,
                        false,
                        List.of(new HitResult(3, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly("⚔️ [윈드밀] 🛡️ 적 방어에 막힘 ➔ 3 피해", "[너구리] 🛡️ 방어 성공 & 반격 ➔ 7 피해 피격");
    }

    @Test
    @DisplayName("내 방어 승리 시 플레이어 반격 + 몬스터 경감 공격 로그를 대칭으로 남긴다")
    void should_logPlayerCounter_when_playerDefenseWins() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        9,
                        6,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly(
                        "🛡️ [디펜스] 방어 성공 & 반격! ➔ 너구리에게 9 반격 피해", "[너구리] 일반공격 ➔ 🛡️ 방어로 경감되어 6 피해");
    }

    @Test
    @DisplayName("내 강공격이 몬스터 방어를 관통하면 몬스터 방어 뚫림 로그를 남긴다")
    void should_logMonsterDefenseBroken_when_heavyPenetratesMonsterDefense() {
        final BattleLogInput input =
                new BattleLogInput(
                        HEAVY_SKILL,
                        SkillType.HEAVY,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        18,
                        0,
                        false,
                        false,
                        false,
                        List.of(new HitResult(18, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [스매시] ➔ 너구리에게 18 피해", "[너구리] 💥 방어선 관통됨!");
    }

    @Test
    @DisplayName("내 방어가 몬스터 강공격에 관통당하면 방어 뚫림 로그를 남긴다")
    void should_logPlayerDefenseBroken_when_monsterHeavyPenetrates() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.HEAVY,
                        0,
                        20,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚠️ [디펜스] 몬스터 강공격에 방어선 관통!", "[너구리] 강공격 ➔ 20 피해 피격");
    }

    @Test
    @DisplayName("양쪽 모두 방어하면 교착 + 방어 태세 로그를 남긴다")
    void should_logStalemate_when_bothDefend() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        0,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🛡️ [디펜스] 맞방어 교착 상태", "[너구리] 🛡️ 방어 태세 유지");
    }

    @Test
    @DisplayName("활 1턴 선제 사격(단일 히트)은 선제 공격 형식 한 줄만 남긴다")
    void should_logSingleFirstStrikeLine_when_firstStrike() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        12,
                        0,
                        false,
                        true,
                        false,
                        List.of(new HitResult(12, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [선제 공격] [윈드밀] ➔ 너구리에게 12 피해");
    }

    @Test
    @DisplayName("선제 공격 기회에서 방어 스킬 사용 시 0 피해와 방어 태세 로그를 남긴다")
    void should_logDefenseStance_when_firstStrikeWithDefenseSkill() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        0,
                        false,
                        true,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [선제 공격] 선제 찬스였으나 [디펜스] 태세 유지");
    }

    @Test
    @DisplayName("마법 캐스팅 실패 시 몬스터 공격 로그만 남긴다")
    void should_logOnlyMonsterAttack_when_castFailureAndMonsterAttacks() {
        final BattleLogInput input =
                new BattleLogInput(
                        MAGIC_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        13,
                        false,
                        false,
                        true,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("[너구리] 일반공격 ➔ 13 피해 피격");
    }

    @Test
    @DisplayName("마법 캐스팅 실패 중 몬스터가 방어하면 방어 태세 로그만 남긴다")
    void should_logOnlyMonsterDefenseStance_when_castFailureAndMonsterDefends() {
        final BattleLogInput input =
                new BattleLogInput(
                        MAGIC_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        0,
                        0,
                        false,
                        false,
                        true,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("[너구리] 🛡️ 방어 태세 유지");
    }

    @Test
    @DisplayName("멀티히트(3타) 시 통합 연타 포맷 로그를 생성한다")
    void should_logMultiHitHeaderAndBreakdown_when_threeHits() {
        final List<HitResult> hits =
                List.of(
                        new HitResult(22, false),
                        new HitResult(33, true),
                        new HitResult(19, false));
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        74,
                        10,
                        false,
                        false,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly(
                        "⚔️ [윈드밀] 3연타 (22 · 33💥 · 19) ➔ 총 74 피해", "[너구리] 일반공격 ➔ 10 피해 피격");
    }

    @Test
    @DisplayName("멀티히트(4타) 선제 사격 시 선제 통합 연타 포맷 로그를 생성한다")
    void should_logMultiHitFirstStrike_when_fourHitsFirstStrike() {
        final List<HitResult> hits =
                List.of(
                        new HitResult(10, false), new HitResult(12, true),
                        new HitResult(9, false), new HitResult(11, false));
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        42,
                        0,
                        false,
                        true,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [선제 공격] [윈드밀] 4연타 (10 · 12💥 · 9 · 11) ➔ 총 42 피해");
    }

    @Test
    @DisplayName("멀티히트에서 모든 히트가 크리티컬이면 모든 값에 💥 접미사가 붙는다")
    void should_appendCriticalToAllHits_when_allCritical() {
        final List<HitResult> hits =
                List.of(new HitResult(30, true), new HitResult(28, true), new HitResult(32, true));
        final BattleLogInput input =
                new BattleLogInput(
                        HEAVY_SKILL,
                        SkillType.HEAVY,
                        MONSTER_NAME,
                        SkillType.HEAVY,
                        90,
                        0,
                        false,
                        false,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly("⚔️ [스매시] 3연타 (30💥 · 28💥 · 32💥) ➔ 총 90 피해", "[너구리] 강공격 ➔ 빗나감");
    }

    @Test
    @DisplayName("디펜스 100% 완전 방어 시 완전 방어 성공 로그를 남긴다")
    void should_logCompleteDefense_when_defenseBlocksCompletely() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly(
                        "🛡️ [디펜스] 완벽 방어! ➔ 빈틈 포착 (다음 턴 선제 찬스⚡)", "[너구리] 일반공격 ➔ 🛡️ 방어로 경감되어 0 피해");
    }

    @Test
    @DisplayName("몬스터 방어 시 플레이어 일반 공격이 막히면 방어 가로막힘 로그를 남긴다")
    void should_logAttackBlocked_when_playerNormalBlockedByMonsterDefense() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        0,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly(
                        "⚔️ [윈드밀] 🛡️ 너구리의 완전 방어에 가로막힘 (0 피해)",
                        "[너구리] 🛡️ 공격 방어 성공 ➔ 반격 태세 (다음 턴 선제 주의⚠️)");
    }

    @Test
    @DisplayName("카운터 어택 성공 시 회피 및 치명적 반격 로그를 남긴다")
    void should_logCounterAttackSuccess_when_counterHits() {
        final BattleLogInput input =
                new BattleLogInput(
                        "카운터 어택",
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        45,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly("⚡ [카운터 어택] 💥 적 공격을 흘려내며 ➔ 45 치명 반격!", "[너구리] 일반공격 ➔ 빗나감");
    }

    @Test
    @DisplayName("카운터 어택 시 몬스터가 방어하면 헛방 로그를 남긴다")
    void should_logCounterAttackMiss_when_monsterDefends() {
        final BattleLogInput input =
                new BattleLogInput(
                        "카운터 어택",
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        0,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [카운터 어택] 적이 공격하지 않아 빗나감", "[너구리] 🛡️ 방어 태세 유지");
    }

    @Test
    @DisplayName("궁극기 멀티히트 시 절대 우위 100% 관통 통합 연타 로그를 남긴다")
    void should_logUltimateMultiHit_withSuperPriorityHeader() {
        final BattleLogInput input =
                new BattleLogInput(
                        "파이널 히트",
                        SkillType.ULTIMATE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        250,
                        0,
                        false,
                        true,
                        false,
                        List.of(
                                new HitResult(50, false),
                                new HitResult(50, false),
                                new HitResult(50, true),
                                new HitResult(50, false),
                                new HitResult(50, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly(
                        "👑 [결전 궁극기] [파이널 히트] 5연타 (50 · 50 · 50💥 · 50 · 50) ➔ 총 250 관통 피해");
    }

    @Test
    @DisplayName("궁극기 단일히트 시 절대 우위 100% 관통 단일 로그를 남긴다")
    void should_logUltimateSingleHit_withSuperPriorityHeader() {
        final BattleLogInput input =
                new BattleLogInput(
                        "메테오 스트라이크",
                        SkillType.ULTIMATE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        500,
                        0,
                        true,
                        true,
                        false,
                        List.of(new HitResult(500, true)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("👑 [결전 궁극기] [메테오 스트라이크] 💥 100% 관통 ➔ 500 피해");
    }

    @Test
    @DisplayName("선제공격 시 대미지가 0이면 태세 유지 로그를 남긴다")
    void should_logFirstStrikeStance_when_playerDamageIsZero() {
        final BattleLogInput input =
                new BattleLogInput(
                        DEFENSE_SKILL,
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        0,
                        false,
                        true,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [선제 공격] 선제 찬스였으나 [디펜스] 태세 유지");
    }

    @Test
    @DisplayName("카운터 어택 성공 시 치명 반격 로그를 남긴다")
    void should_logCounterAttackSuccess() {
        final BattleLogInput input =
                new BattleLogInput(
                        "카운터 어택",
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        30,
                        0,
                        true,
                        false,
                        false,
                        List.of(new HitResult(30, true)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines)
                .containsExactly("⚡ [카운터 어택] 💥 적 공격을 흘려내며 ➔ 30 치명 반격!", "[너구리] 일반공격 ➔ 빗나감");
    }

    @Test
    @DisplayName("카운터 어택 시 적이 공격하지 않으면 빗나감 로그를 남긴다")
    void should_logCounterAttackMiss_when_monsterDoesNotAttack() {
        final BattleLogInput input =
                new BattleLogInput(
                        "카운터 어택",
                        SkillType.DEFENSE,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        0,
                        0,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚡ [카운터 어택] 적이 공격하지 않아 빗나감", "[너구리] 🛡️ 방어 태세 유지");
    }

    @Test
    @DisplayName("몬스터 방어 시 플레이어 강공격에 방어선 관통 로그를 남긴다")
    void should_logMonsterDefensePierced_when_playerDealsDamage() {
        final BattleLogInput input =
                new BattleLogInput(
                        HEAVY_SKILL,
                        SkillType.HEAVY,
                        MONSTER_NAME,
                        SkillType.DEFENSE,
                        40,
                        0,
                        false,
                        false,
                        false,
                        List.of(new HitResult(40, false)));

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("🗡️ [스매시] ➔ 너구리에게 40 피해", "[너구리] 💥 방어선 관통됨!");
    }

    @Test
    @DisplayName("적 선제공격 시 기습 로그를 남긴다")
    void should_logEnemyFirstStrike_when_monsterDamagePositiveAndPlayerZero() {
        final BattleLogInput input =
                new BattleLogInput(
                        ATTACK_SKILL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        25,
                        false,
                        true,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        assertThat(lines).containsExactly("⚠️ [적 선제공격] [너구리] 기습 ➔ 25 피해 피격");
    }
}
