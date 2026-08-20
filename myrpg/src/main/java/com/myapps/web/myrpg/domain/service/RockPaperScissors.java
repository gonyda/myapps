package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.AffinityResult;
import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 가위바위보 상성 판정을 수행하는 순수 유틸리티 클래스.
 *
 * <p>일반(NORMAL) &gt; 강(HEAVY) &gt; 방어(DEFENSE) &gt; 일반(NORMAL) 순환 상성을 따르며, 동일 타입은 DRAW를 반환한다. 인스턴스를
 * 생성하지 않는 정적 메서드만 제공한다.
 */
public final class RockPaperScissors {

    private RockPaperScissors() {
        // 인스턴스 생성 방지
    }

    /**
     * 두 스킬 타입 간 상성을 판정한다.
     *
     * <p>상성 규칙:
     *
     * <ul>
     *   <li>일반(NORMAL) &gt; 강(HEAVY): 일반 승리
     *   <li>강(HEAVY) &gt; 방어(DEFENSE): 강 승리
     *   <li>방어(DEFENSE) &gt; 일반(NORMAL): 방어 승리
     *   <li>동일 타입: 무승부(DRAW)
     * </ul>
     *
     * @param mine 자신의 스킬 타입
     * @param other 상대의 스킬 타입
     * @return 자신 기준 상성 결과 ({@link AffinityResult#WIN}, {@link AffinityResult#LOSE}, {@link
     *     AffinityResult#DRAW})
     */
    public static AffinityResult judge(final SkillType mine, final SkillType other) {
        if (mine == other) {
            return AffinityResult.DRAW;
        }
        return switch (mine) {
            case NORMAL -> other == SkillType.HEAVY ? AffinityResult.WIN : AffinityResult.LOSE;
            case HEAVY -> other == SkillType.DEFENSE ? AffinityResult.WIN : AffinityResult.LOSE;
            case DEFENSE -> other == SkillType.NORMAL ? AffinityResult.WIN : AffinityResult.LOSE;
        };
    }
}
