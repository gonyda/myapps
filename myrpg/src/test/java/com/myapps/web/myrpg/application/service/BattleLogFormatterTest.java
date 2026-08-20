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

        assertThat(lines).containsExactly("윈드밀(일반)로 너구리에게 10 피해", "너구리의 일반공격, 13 피해를 입음");
    }

    @Test
    @DisplayName("크리티컬 시 플레이어 피해 로그에 (크리티컬!)을 붙인다")
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

        assertThat(lines).containsExactly("윈드밀(일반)로 너구리에게 15 피해 (크리티컬!)", "너구리의 강공격이 빗나갔다!");
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

        assertThat(lines).containsExactly("스매시(강) 공격이 빗나갔다!", "너구리의 일반공격, 13 피해를 입음");
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

        assertThat(lines).containsExactly("윈드밀(일반)로 너구리에게 10 피해", "너구리의 강공격이 빗나갔다!");
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

        assertThat(lines).containsExactly("윈드밀(일반)로 너구리에게 3 피해", "너구리이(가) 방어하며 반격! (7 피해)");
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

        assertThat(lines).containsExactly("디펜스(방어)로 방어하며 반격! (9 피해)", "너구리의 일반공격, 6 피해를 입음");
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

        assertThat(lines).containsExactly("스매시(강)로 너구리에게 18 피해", "너구리의 방어가 뚫렸다!");
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

        assertThat(lines).containsExactly("디펜스(방어) 방어가 뚫렸다!", "너구리의 강공격, 20 피해를 입음");
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

        assertThat(lines).containsExactly("디펜스(방어)로 맞서 교착 상태!", "너구리이(가) 방어 태세를 취했다.");
    }

    @Test
    @DisplayName("활 1턴 선제 사격(단일 히트)은 기존 형식 한 줄만 남긴다")
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

        assertThat(lines).containsExactly("선제 사격! 윈드밀(일반)로 너구리에게 12 피해");
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

        assertThat(lines).containsExactly("너구리의 일반공격, 13 피해를 입음");
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

        assertThat(lines).containsExactly("너구리이(가) 방어 태세를 취했다.");
    }

    @Test
    @DisplayName("멀티히트(3타) 시 헤더와 브레이크다운을 생성한다")
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
                .containsExactly("윈드밀(일반) 3연타", "22  33(치명)  19 = 74 피해", "너구리의 일반공격, 10 피해를 입음");
    }

    @Test
    @DisplayName("멀티히트(4타) 선제 사격 시 헤더와 브레이크다운을 생성한다")
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

        assertThat(lines).containsExactly("선제 사격! 윈드밀(일반) 4연타", "10  12(치명)  9  11 = 42 피해");
    }

    @Test
    @DisplayName("멀티히트에서 모든 히트가 크리티컬이면 모든 값에 (치명) 접미사가 붙는다")
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
                .containsExactly("스매시(강) 3연타", "30(치명)  28(치명)  32(치명) = 90 피해", "너구리의 강공격이 빗나갔다!");
    }
}
