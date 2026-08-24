package com.myapps.web.myrpg.domain.model;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * 스킬 랭크업 요구치 및 AP(능력치 포인트) 소모 정책을 정의하는 순수 도메인 클래스.
 *
 * <p>각 랭크에서 다음 랭크로 승급하기 위해 필요한 사용 횟수·막타 처치 수와 AP 비용을 상수 테이블로 보유한다. {@code MASTER}(최고 랭크)에는 승급이 없으므로
 * 빈 값을 반환한다.
 *
 * <p>요구치와 AP 비용은 랭크 순서({@link SkillRank#order()})가 오름에 따라 단조 증가하며, F→Master 전체 AP 소모 합계는 200이다.
 *
 * <p>이 클래스는 Spring 빈이 아닌 순수 정책 객체로 사용된다({@code ExperiencePolicy} 선례).
 */
public class SkillRankPolicy {

    private static final RankUpRequirement[] REQUIREMENTS = {
        new RankUpRequirement(5, 1), // F → E
        new RankUpRequirement(10, 3), // E → D
        new RankUpRequirement(20, 6), // D → C
        new RankUpRequirement(35, 10), // C → B
        new RankUpRequirement(60, 18), // B → A
        new RankUpRequirement(100, 30), // A → 9
        new RankUpRequirement(160, 48), // 9 → 8
        new RankUpRequirement(240, 72), // 8 → 7
        new RankUpRequirement(350, 105), // 7 → 6
        new RankUpRequirement(520, 155), // 6 → 5
        new RankUpRequirement(760, 230), // 5 → 4
        new RankUpRequirement(1100, 340), // 4 → 3
        new RankUpRequirement(1600, 500), // 3 → 2
        new RankUpRequirement(2500, 750), // 2 → 1
        new RankUpRequirement(5000, 1500) // 1 → Master
    };

    private static final RankUpRequirement[] ULTIMATE_REQUIREMENTS = {
        new RankUpRequirement(1, 0), // F → E
        new RankUpRequirement(2, 0), // E → D
        new RankUpRequirement(3, 0), // D → C
        new RankUpRequirement(4, 0), // C → B
        new RankUpRequirement(5, 0), // B → A
        new RankUpRequirement(6, 0), // A → 9
        new RankUpRequirement(7, 0), // 9 → 8
        new RankUpRequirement(8, 0), // 8 → 7
        new RankUpRequirement(9, 0), // 7 → 6
        new RankUpRequirement(10, 0), // 6 → 5
        new RankUpRequirement(12, 0), // 5 → 4
        new RankUpRequirement(14, 0), // 4 → 3
        new RankUpRequirement(16, 0), // 3 → 2
        new RankUpRequirement(18, 0), // 2 → 1
        new RankUpRequirement(20, 0) // 1 → Master
    };

    private static final int[] AP_COSTS = {
        1, // F → E
        2, // E → D
        3, // D → C
        4, // C → B
        5, // B → A
        7, // A → 9
        9, // 9 → 8
        11, // 8 → 7
        13, // 7 → 6
        15, // 6 → 5
        18, // 5 → 4
        22, // 4 → 3
        26, // 3 → 2
        30, // 2 → 1
        34 // 1 → Master
    };

    /**
     * 지정된 랭크에서 다음 랭크로 승급하기 위한 표준 요구 조건을 반환한다.
     *
     * <p>{@code MASTER}는 최고 랭크이므로 빈 값을 반환한다.
     *
     * @param current 현재 스킬 랭크
     * @return 다음 랭크 승급 조건의 {@link Optional}, {@code MASTER}이면 {@link Optional#empty()}
     */
    public Optional<RankUpRequirement> requirement(final SkillRank current) {
        if (current.isMax()) {
            return Optional.empty();
        }
        return Optional.of(REQUIREMENTS[current.order()]);
    }

    /**
     * 지정된 랭크에서 궁극기(ULTIMATE) 승급을 위한 전용 요구 조건(1~20회 사용, 막타 0)을 반환한다.
     *
     * @param current 현재 스킬 랭크
     * @return 궁극기 승급 조건의 {@link Optional}, {@code MASTER}이면 {@link Optional#empty()}
     */
    public Optional<RankUpRequirement> ultimateRequirement(final SkillRank current) {
        if (current.isMax()) {
            return Optional.empty();
        }
        return Optional.of(ULTIMATE_REQUIREMENTS[current.order()]);
    }

    /**
     * 스킬 타입별 승급 요구 조건을 반환한다.
     *
     * @param current 현재 스킬 랭크
     * @param type 스킬 타입
     * @return 해당 타입 승급 조건의 {@link Optional}
     */
    public Optional<RankUpRequirement> requirementFor(
            final SkillRank current, final SkillType type) {
        if (current.isMax()) {
            return Optional.empty();
        }
        if (type == SkillType.ULTIMATE) {
            return ultimateRequirement(current);
        }
        if (type == SkillType.PASSIVE) {
            return Optional.of(new RankUpRequirement(0, 0));
        }
        return requirement(current);
    }

    /**
     * 지정된 랭크에서 다음 랭크로 승급할 때 소모되는 AP를 반환한다.
     *
     * <p>{@code MASTER}는 최고 랭크이므로 빈 값을 반환한다. F→Master 전체 AP 소모 합계는 200이다.
     *
     * @param current 현재 스킬 랭크
     * @return AP 비용의 {@link OptionalInt}, {@code MASTER}이면 {@link OptionalInt#empty()}
     */
    public OptionalInt apCost(final SkillRank current) {
        if (current.isMax()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(AP_COSTS[current.order()]);
    }
}
