package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.BattleLogInput;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 멀티히트 로그 포맷을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 {@code playerHits}에 대해, size ≥ 2면 헤더({@code "{스킬}({타입}) {N}연타"})와 브레이크다운(각 히트 피해 나열, 크리
 * 히트에 {@code "(치명)"}, 끝에 {@code "= {합계} 피해"})을 생성하고, size ≤ 1이면 기존 단일 형식({@code "{스킬}({타입})로 … {N}
 * 피해"} (+{@code " (크리티컬!)"}))을 생성한다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 7: 멀티히트 로그 포맷
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3, 6.5</b>
 */
class BattleLogFormatterMultiHitPropertyTest {

    private static final String SKILL_LABEL = "윈드밀";
    private static final String MONSTER_NAME = "늑대";
    private static final int MULTI_HIT_THRESHOLD = 2;
    private static final String HIT_SEPARATOR = " · ";
    private static final String CRITICAL_SUFFIX = "💥";
    private static final String ARROW = " ➔ ";

    private final BattleLogFormatter formatter = new BattleLogFormatter();

    /**
     * playerHits.size() ≥ 2일 때, 플레이어 로그가 1줄 통합 연타 형식("{스킬}({타입}) {N}연타 ({d1} · {d2}💥) ➔ {합계}
     * 피해")으로 구성되는지 검증한다.
     *
     * @param hits 히트 결과 리스트(size 2~8)
     * @param skillType 플레이어 스킬 타입 (NORMAL 또는 HEAVY)
     * @param monsterDamage 몬스터 피해
     */
    @Property(tries = 100)
    void should_produceHeaderAndBreakdown_when_multiHit(
            @ForAll("multiHitResults") final List<HitResult> hits,
            @ForAll("attackSkillTypes") final SkillType skillType,
            @ForAll("monsterDamages") final int monsterDamage) {

        final int totalDamage = hits.stream().mapToInt(HitResult::damage).sum();
        final BattleLogInput input =
                new BattleLogInput(
                        SKILL_LABEL,
                        skillType,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        totalDamage,
                        monsterDamage,
                        false,
                        false,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        // 플레이어 행동: 1줄, 몬스터 행동 1줄 = 총 2줄
        assertThat(lines).hasSize(2);

        // 통합 연타 포맷 검증
        final String expectedPlayerLine =
                SKILL_LABEL
                        + "("
                        + skillType.label()
                        + ") "
                        + hits.size()
                        + "연타 ("
                        + formatHits(hits)
                        + ")"
                        + ARROW
                        + totalDamage
                        + " 피해";
        assertThat(lines.get(0)).isEqualTo(expectedPlayerLine);
    }

    /**
     * playerHits.size() ≥ 2이고 firstStrike일 때, 선제 사격 멀티히트 "선제 공격! {스킬}({타입}) {N}연타 ({d1} · {d2}💥) ➔
     * {합계} 피해"가 생성되는지 검증한다.
     *
     * @param hits 히트 결과 리스트(size 2~8)
     * @param skillType 플레이어 스킬 타입 (NORMAL 또는 HEAVY)
     */
    @Property(tries = 100)
    void should_produceFirstStrikeMultiHitFormat_when_firstStrikeAndMultiHit(
            @ForAll("multiHitResults") final List<HitResult> hits,
            @ForAll("attackSkillTypes") final SkillType skillType) {

        final int totalDamage = hits.stream().mapToInt(HitResult::damage).sum();
        final BattleLogInput input =
                new BattleLogInput(
                        SKILL_LABEL,
                        skillType,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        totalDamage,
                        0,
                        false,
                        true,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        // 선제 사격 멀티히트: 1줄
        assertThat(lines).hasSize(1);

        // 헤더 검증: "선제 공격! {스킬}({타입}) {N}연타 (...)"
        final String expectedLine =
                "선제 공격! "
                        + SKILL_LABEL
                        + "("
                        + skillType.label()
                        + ") "
                        + hits.size()
                        + "연타 ("
                        + formatHits(hits)
                        + ")"
                        + ARROW
                        + totalDamage
                        + " 피해";
        assertThat(lines.get(0)).isEqualTo(expectedLine);
    }

    /**
     * playerHits.size() ≤ 1일 때, 플레이어 로그가 단일 형식 1줄 ("{스킬}({타입})로 {몬스터}에게 {N} 피해"(+크리티컬))로 생성되는지
     * 검증한다.
     *
     * @param damage 플레이어 피해
     * @param critical 크리티컬 여부
     * @param skillType 플레이어 스킬 타입 (NORMAL 또는 HEAVY)
     * @param monsterDamage 몬스터 피해
     */
    @Property(tries = 100)
    void should_produceSingleLineFormat_when_singleHit(
            @ForAll("singleDamages") final int damage,
            @ForAll("booleans") final boolean critical,
            @ForAll("attackSkillTypes") final SkillType skillType,
            @ForAll("monsterDamages") final int monsterDamage) {

        final List<HitResult> hits = List.of(new HitResult(damage, critical));
        final BattleLogInput input =
                new BattleLogInput(
                        SKILL_LABEL,
                        skillType,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        damage,
                        monsterDamage,
                        critical,
                        false,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);

        // 단일 히트: 플레이어 1줄 + 몬스터 1줄 = 2줄
        assertThat(lines).hasSize(2);

        // 단일 형식: "{스킬}({타입})로 {몬스터}에게 {N} 피해" (+ " (크리티컬!)")
        final String baseLine =
                SKILL_LABEL
                        + "("
                        + skillType.label()
                        + ")로 "
                        + MONSTER_NAME
                        + "에게 "
                        + damage
                        + " 피해";
        final String expectedPlayerLine = critical ? baseLine + " (크리티컬!)" : baseLine;
        assertThat(lines.get(0)).isEqualTo(expectedPlayerLine);
    }

    /**
     * playerHits가 빈 리스트이고 playerDamage가 0일 때, 빗나감 형식으로 생성되는지 검증한다.
     *
     * @param skillType 플레이어 스킬 타입 (NORMAL 또는 HEAVY)
     * @param monsterDamage 몬스터 피해
     */
    @Property(tries = 100)
    void should_produceMissFormat_when_emptyHitsAndZeroDamage(
            @ForAll("attackSkillTypes") final SkillType skillType,
            @ForAll("monsterDamages") final int monsterDamage) {

        final BattleLogInput input =
                new BattleLogInput(
                        SKILL_LABEL,
                        skillType,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        0,
                        monsterDamage,
                        false,
                        false,
                        false,
                        List.of());

        final List<String> lines = formatter.combatLines(input);

        // 빗나감 + 몬스터 = 2줄
        assertThat(lines).hasSize(2);

        // 빗나감 형식
        final String expectedMissLine = SKILL_LABEL + "(" + skillType.label() + ") 공격이 빗나갔다!";
        assertThat(lines.get(0)).isEqualTo(expectedMissLine);
    }

    /**
     * 연타 라인의 각 히트 값이 올바르게 "💥" 접미사를 가지며 합계가 정확히 일치하는지 검증한다.
     *
     * @param hits 히트 결과 리스트(size 2~8)
     */
    @Property(tries = 100)
    void should_containCorrectCriticalSuffixes_when_multiHit(
            @ForAll("multiHitResults") final List<HitResult> hits) {

        final int totalDamage = hits.stream().mapToInt(HitResult::damage).sum();
        final BattleLogInput input =
                new BattleLogInput(
                        SKILL_LABEL,
                        SkillType.NORMAL,
                        MONSTER_NAME,
                        SkillType.NORMAL,
                        totalDamage,
                        5,
                        false,
                        false,
                        false,
                        hits);

        final List<String> lines = formatter.combatLines(input);
        final String playerLine = lines.get(0);

        // 연타 라인은 "➔ {합계} 피해"로 끝나야 한다
        assertThat(playerLine).endsWith(ARROW + totalDamage + " 피해");

        // 각 크리티컬 히트는 해당 값 뒤에 "💥"이 있어야 한다
        for (final HitResult hit : hits) {
            if (hit.critical()) {
                assertThat(playerLine).contains(hit.damage() + CRITICAL_SUFFIX);
            }
        }
    }

    /**
     * 히트별 포맷 문자열을 구성한다.
     *
     * @param hits 히트 결과 리스트
     * @return 포맷 문자열
     */
    private String formatHits(final List<HitResult> hits) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            if (i > 0) {
                builder.append(HIT_SEPARATOR);
            }
            final HitResult hit = hits.get(i);
            builder.append(hit.damage());
            if (hit.critical()) {
                builder.append(CRITICAL_SUFFIX);
            }
        }
        return builder.toString();
    }

    /**
     * 멀티히트 결과 리스트 생성기 (size 2~8, 각 히트 damage 1~99, critical 랜덤).
     *
     * @return HitResult 리스트 Arbitrary
     */
    @Provide
    Arbitrary<List<HitResult>> multiHitResults() {
        final Arbitrary<HitResult> hitArbitrary =
                Combinators.combine(
                                Arbitraries.integers().between(1, 99), Arbitraries.of(true, false))
                        .as(HitResult::new);
        return hitArbitrary.list().ofMinSize(MULTI_HIT_THRESHOLD).ofMaxSize(8);
    }

    /**
     * 공격용 스킬 타입 생성기 (NORMAL 또는 HEAVY, DEFENSE 제외).
     *
     * @return SkillType Arbitrary
     */
    @Provide
    Arbitrary<SkillType> attackSkillTypes() {
        return Arbitraries.of(SkillType.NORMAL, SkillType.HEAVY);
    }

    /**
     * 몬스터 피해 생성기 (0~50).
     *
     * @return 몬스터 피해 Arbitrary
     */
    @Provide
    Arbitrary<Integer> monsterDamages() {
        return Arbitraries.integers().between(0, 50);
    }

    /**
     * 단일 히트 피해 생성기 (1~200).
     *
     * @return 단일 히트 피해 Arbitrary
     */
    @Provide
    Arbitrary<Integer> singleDamages() {
        return Arbitraries.integers().between(1, 200);
    }

    /**
     * 불리언 생성기.
     *
     * @return Boolean Arbitrary
     */
    @Provide
    Arbitrary<Boolean> booleans() {
        return Arbitraries.of(true, false);
    }
}
