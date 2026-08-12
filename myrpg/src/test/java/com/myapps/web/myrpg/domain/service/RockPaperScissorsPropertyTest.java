package com.myapps.web.myrpg.domain.service;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Tuple;

import com.myapps.web.myrpg.domain.model.AffinityResult;
import com.myapps.web.myrpg.domain.model.SkillType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가위바위보 상성 판정의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>모든 {@link SkillType} 조합에 대해 {@link RockPaperScissors#judge}가
 * 일반&gt;강, 강&gt;방어, 방어&gt;일반 순환 상성을 따르며, 대칭(역관계)이 성립하는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 1: 가위바위보 상성
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b>
 */
class RockPaperScissorsPropertyTest {

    /**
     * 일반(NORMAL)이 강(HEAVY)을 이기는지 검증한다.
     */
    @Property(tries = 100)
    void should_returnWin_when_normalVsHeavy() {
        final AffinityResult result = RockPaperScissors.judge(SkillType.NORMAL, SkillType.HEAVY);
        assertThat(result).isEqualTo(AffinityResult.WIN);
    }

    /**
     * 강(HEAVY)이 방어(DEFENSE)를 이기는지 검증한다.
     */
    @Property(tries = 100)
    void should_returnWin_when_heavyVsDefense() {
        final AffinityResult result = RockPaperScissors.judge(SkillType.HEAVY, SkillType.DEFENSE);
        assertThat(result).isEqualTo(AffinityResult.WIN);
    }

    /**
     * 방어(DEFENSE)가 일반(NORMAL)을 이기는지 검증한다.
     */
    @Property(tries = 100)
    void should_returnWin_when_defenseVsNormal() {
        final AffinityResult result = RockPaperScissors.judge(SkillType.DEFENSE, SkillType.NORMAL);
        assertThat(result).isEqualTo(AffinityResult.WIN);
    }

    /**
     * 상성 역방향은 LOSE를 반환하는지 검증한다.
     */
    @Property(tries = 100)
    void should_returnLose_when_reverseAffinity() {
        assertThat(RockPaperScissors.judge(SkillType.HEAVY, SkillType.NORMAL))
                .isEqualTo(AffinityResult.LOSE);
        assertThat(RockPaperScissors.judge(SkillType.DEFENSE, SkillType.HEAVY))
                .isEqualTo(AffinityResult.LOSE);
        assertThat(RockPaperScissors.judge(SkillType.NORMAL, SkillType.DEFENSE))
                .isEqualTo(AffinityResult.LOSE);
    }

    /**
     * 동일 타입끼리는 항상 DRAW를 반환하는지 검증한다.
     *
     * @param type 임의의 스킬 타입
     */
    @Property(tries = 100)
    void should_returnDraw_when_sameType(@ForAll("skillTypes") final SkillType type) {
        final AffinityResult result = RockPaperScissors.judge(type, type);
        assertThat(result).isEqualTo(AffinityResult.DRAW);
    }

    /**
     * 대칭 역관계를 검증한다: judge(a,b)==WIN ↔ judge(b,a)==LOSE, DRAW ↔ DRAW.
     *
     * @param pair 임의의 두 스킬 타입 조합
     */
    @Property(tries = 100)
    void should_satisfySymmetry_when_anyTwoTypes(
            @ForAll("skillTypePairs") final Tuple.Tuple2<SkillType, SkillType> pair) {

        final SkillType mine = pair.get1();
        final SkillType other = pair.get2();

        final AffinityResult forward = RockPaperScissors.judge(mine, other);
        final AffinityResult reverse = RockPaperScissors.judge(other, mine);

        switch (forward) {
            case WIN -> assertThat(reverse).isEqualTo(AffinityResult.LOSE);
            case LOSE -> assertThat(reverse).isEqualTo(AffinityResult.WIN);
            case DRAW -> assertThat(reverse).isEqualTo(AffinityResult.DRAW);
        }
    }

    /**
     * 모든 스킬 타입을 생성하는 Arbitrary.
     *
     * @return SkillType Arbitrary
     */
    @Provide
    Arbitrary<SkillType> skillTypes() {
        return Arbitraries.of(SkillType.NORMAL, SkillType.HEAVY, SkillType.DEFENSE);
    }

    /**
     * 두 스킬 타입의 모든 조합을 생성하는 Arbitrary.
     *
     * @return 두 SkillType 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<SkillType, SkillType>> skillTypePairs() {
        final Arbitrary<SkillType> types = Arbitraries.of(
                SkillType.NORMAL, SkillType.HEAVY, SkillType.DEFENSE);
        return types.flatMap(mine -> types.map(other -> Tuple.of(mine, other)));
    }
}
