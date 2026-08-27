package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.AffinityResult;
import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 전투 데미지 계산을 담당하는 순수 도메인 서비스.
 *
 * <p>감산형 데미지 공식, 가위바위보 상성계수, 크리티컬 판정, 편차를 조합하여 9칸 매트릭스 기반의 양측 피해를 산출한다. 결정적 부분(감산·상성·경감·반격)은 순수
 * 함수이며, 비결정적 부분(크리티컬·편차)만 주입된 {@link Random}을 사용한다.
 *
 * <p>멀티히트 지원: 딜 스킬의 {@code hitCount}만큼 히트를 반복하며, 각 히트마다 방어 차감·크리티컬·편차를 독립적으로 산출한다.
 *
 * <p>선후공 결정은 이 클래스의 책임이 아니며 상위 서비스(BattleService)가 처리한다.
 */
public class BattleResolver {

    /** 크리티컬 발동 시 데미지 배율. */
    private static final double CRITICAL_MULTIPLIER = 1.5;

    /** 상성 무승부 시 데미지 계수 (양쪽 50%). */
    private static final double DRAW_COEFFICIENT = 0.5;

    /** 상성 승리 시 데미지 계수 (100% 적중). */
    private static final double WIN_COEFFICIENT = 1.0;

    /** 관통(강 vs 방어에서 방어측) 시 데미지 계수 (반격 무효). */
    private static final double PENETRATED_COEFFICIENT = 0.0;

    /** 편차 범위: random.nextInt(201) → 0~200. */
    private static final int VARIANCE_RANGE_MILLIS = 200;

    /** 편차 기저: (900 + roll) / 1000.0 → 0.90 ~ 1.10. */
    private static final int VARIANCE_BASE_MILLIS = 900;

    /** 편차 산출 시 나누기 값. */
    private static final double MILLIS_DIVISOR = 1000.0;

    /** 크리티컬 판정 최대 범위 (0~999). */
    private static final int CRITICAL_ROLL_MAX = 1000;

    private final Random random;

    /**
     * {@code BattleResolver}를 생성한다.
     *
     * @param random 크리티컬 판정 및 편차 산출에 사용할 난수 생성기
     */
    public BattleResolver(final Random random) {
        this.random = random;
    }

    /**
     * 감산형 기본피해를 계산한다.
     *
     * <p>공식: {@code max(1, floor(attackPower × skillMultiplierPercent / 100) − targetDefense)}. 방어가
     * 산출 피해를 초과해도 최소 1을 보장한다.
     *
     * @param attackPower 공격력
     * @param skillMultiplierPercent 스킬 배율(%)
     * @param targetDefense 대상 방어력
     * @return 기본피해 (최소 1)
     */
    public int baseDamage(
            final int attackPower, final int skillMultiplierPercent, final int targetDefense) {
        final int rawDamage =
                Math.floorDiv(attackPower * skillMultiplierPercent, 100) - targetDefense;
        return Math.max(1, rawDamage);
    }

    /**
     * 방어 관통 여부를 고려하여 감산형 기본피해를 계산한다.
     *
     * <p>{@code defensePierce == true}이면 대상 방어력을 0으로 계산하여 100% 관통 피해를 산출한다.
     *
     * @param attackPower 공격력
     * @param skillMultiplierPercent 스킬 배율(%)
     * @param targetDefense 대상 방어력
     * @param defensePierce 방어 관통 여부
     * @return 기본피해 (최소 1)
     */
    public int baseDamage(
            final int attackPower,
            final int skillMultiplierPercent,
            final int targetDefense,
            final boolean defensePierce) {
        final int effectiveDefense = defensePierce ? 0 : targetDefense;
        return baseDamage(attackPower, skillMultiplierPercent, effectiveDefense);
    }

    /**
     * 랜덤 다단히트 타격 횟수를 산출한다.
     *
     * @param minHits 최소 타수
     * @param maxHits 최대 타수
     * @return 결정된 타격 횟수 (minHits <= return <= maxHits)
     */
    public int rollHitCount(final int minHits, final int maxHits) {
        if (minHits <= 0 && maxHits <= 0) {
            return 1;
        }
        if (minHits >= maxHits) {
            return Math.max(1, minHits);
        }
        return minHits + random.nextInt(maxHits - minHits + 1);
    }

    /**
     * 상성계수를 반환한다.
     *
     * <p>상성 결과와 방어 관련 플래그에 따라 계수를 결정한다:
     *
     * <ul>
     *   <li>WIN: 1.0 (100% 적중)
     *   <li>DRAW: 0.5 (50% 피해)
     *   <li>LOSE + penetrated: 0.0 (강이 방어를 뚫을 때 방어자 반격 무효)
     *   <li>LOSE + not penetrated: (1 − blockRatePercent / 100) — 방어 경감 후 잔여 피해
     * </ul>
     *
     * @param result 상성 판정 결과
     * @param penetrated 관통 여부 (강 vs 방어에서 방어측이 관통당했는지)
     * @param blockRatePercent 방어 경감률(%) — LOSE이고 비관통일 때만 사용
     * @return 상성계수 (0.0 ~ 1.0)
     */
    public double affinityCoefficient(
            final AffinityResult result, final boolean penetrated, final int blockRatePercent) {
        return switch (result) {
            case WIN -> WIN_COEFFICIENT;
            case DRAW -> DRAW_COEFFICIENT;
            case LOSE -> penetrated ? PENETRATED_COEFFICIENT : 1.0 - blockRatePercent / 100.0;
        };
    }

    /**
     * 크리티컬 판정을 수행한다.
     *
     * <p>주입된 {@link Random}으로 0~999 범위의 값을 뽑아, {@code critical} 수치보다 작으면 크리티컬로 판정한다. critical 단위는
     * 0.1% (예: 100 = 10%).
     *
     * @param critical 크리티컬 수치 (0~1000, 0.1% 단위)
     * @return 크리티컬 발동 시 {@code true}
     */
    public boolean rollCritical(final int critical) {
        return random.nextInt(CRITICAL_ROLL_MAX) < critical;
    }

    /**
     * 최종 피해를 산출한다.
     *
     * <p>공식: {@code max(1, round(baseDamage × affinityCoefficient × (critical ? 1.5 : 1.0) ×
     * variance))}. variance는 0.90 ~ 1.10 범위의 균등 분포이며 주입된 {@link Random}으로 산출한다.
     *
     * @param baseDamage 기본피해
     * @param affinityCoefficient 상성계수
     * @param critical 크리티컬 발동 여부
     * @return 최종 피해 (최소 1)
     */
    public int finalDamage(
            final int baseDamage, final double affinityCoefficient, final boolean critical) {
        if (affinityCoefficient <= 0.0 || baseDamage <= 0) {
            return 0;
        }
        final double critMultiplier = critical ? CRITICAL_MULTIPLIER : 1.0;
        final double variance = rollVariance();
        final double rawDamage = baseDamage * affinityCoefficient * critMultiplier * variance;
        return Math.max(1, (int) Math.round(rawDamage));
    }

    /**
     * 멀티히트 피해를 산출한다 (방어 관통 지원).
     *
     * <p>{@code hitCount}번 반복하여 각 히트마다 감산(방어 차감)·크리티컬·편차를 독립적으로 적용한 결과를 반환한다.
     *
     * @param attackPower 공격력
     * @param perHitMultiplierPercent 1히트당 스킬 배율(%)
     * @param targetDefense 대상 방어력
     * @param affinityCoefficient 상성계수
     * @param critChance 크리티컬 수치 (0~1000)
     * @param hitCount 히트 수 (1 이상)
     * @param defensePierce 방어력 100% 관통 여부
     * @return 각 히트의 피해량과 크리티컬 여부를 담은 리스트 (크기 == hitCount)
     */
    public List<HitResult> multiHitDamage(
            final int attackPower,
            final int perHitMultiplierPercent,
            final int targetDefense,
            final double affinityCoefficient,
            final int critChance,
            final int hitCount,
            final boolean defensePierce) {
        final List<HitResult> hits = new ArrayList<>(hitCount);
        for (int i = 0; i < hitCount; i++) {
            final int base =
                    baseDamage(attackPower, perHitMultiplierPercent, targetDefense, defensePierce);
            final boolean crit = rollCritical(critChance);
            hits.add(new HitResult(finalDamage(base, affinityCoefficient, crit), crit));
        }
        return hits;
    }

    /**
     * 멀티히트 피해를 산출한다 (하위호환: defensePierce=false).
     *
     * @param attackPower 공격력
     * @param perHitMultiplierPercent 1히트당 스킬 배율(%)
     * @param targetDefense 대상 방어력
     * @param affinityCoefficient 상성계수
     * @param critChance 크리티컬 수치 (0~1000)
     * @param hitCount 히트 수 (1 이상)
     * @return 각 히트의 피해량과 크리티컬 여부를 담은 리스트 (크기 == hitCount)
     */
    public List<HitResult> multiHitDamage(
            final int attackPower,
            final int perHitMultiplierPercent,
            final int targetDefense,
            final double affinityCoefficient,
            final int critChance,
            final int hitCount) {
        return multiHitDamage(
                attackPower,
                perHitMultiplierPercent,
                targetDefense,
                affinityCoefficient,
                critChance,
                hitCount,
                false);
    }

    /**
     * 9칸 매트릭스에 따라 양측 피해를 산출한다.
     *
     * <p>플레이어와 몬스터의 스킬 타입 조합에 따라 상성을 판정하고, 각 조합별 규칙(경감·반격·관통·교착)을 적용한 뒤 크리티컬과 편차를 반영한 최종 피해를 반환한다.
     * 선후공 결정은 포함하지 않는다.
     *
     * @param input 턴 해결에 필요한 양측 수치 입력
     * @return 양측 피해와 플래그를 담은 해결 결과
     */
    public ResolvedTurn resolve(final TurnInput input) {
        // 특수 상성: 카운터 어택은 몬스터 강공격(스매시)도 흘려내며 반격
        if (input.playerType() == SkillType.DEFENSE
                && input.isCounterAttack()
                && input.monsterType() == SkillType.HEAVY) {
            return resolveCounterAttackWins(input);
        }

        final AffinityResult playerAffinity =
                RockPaperScissors.judge(input.playerType(), input.monsterType());

        return switch (playerAffinity) {
            case WIN -> resolvePlayerWin(input);
            case LOSE -> resolvePlayerLose(input);
            case DRAW -> resolveDraw(input);
        };
    }

    /**
     * 플레이어 상성 승리 시 피해를 산출한다.
     *
     * <p>규칙:
     *
     * <ul>
     *   <li>일반 &gt; 강: 플레이어 100%, 몬스터 0
     *   <li>강 &gt; 방어: 플레이어 100% 관통, 몬스터 반격 무효(0)
     *   <li>방어 &gt; 일반: 플레이어 반격, 몬스터 경감 후 피해 (카운터 어택은 상대 공격력 비례 반격, 디펜스는 0반격)
     * </ul>
     */
    private ResolvedTurn resolvePlayerWin(final TurnInput input) {
        if (input.playerType() == SkillType.DEFENSE) {
            if (input.isCounterAttack()) {
                return resolveCounterAttackWins(input);
            }
            return resolveDefenseWinsNormal(input);
        }
        return resolveAttackWins(input);
    }

    /**
     * 플레이어 상성 패배 시 피해를 산출한다.
     *
     * <p>규칙:
     *
     * <ul>
     *   <li>일반 &lt; 방어: 플레이어 경감 후 피해, 몬스터 반격
     *   <li>강 &lt; 일반: 플레이어 0, 몬스터 100%
     *   <li>방어 &lt; 강: 플레이어 반격 무효(0), 몬스터 100% 관통
     * </ul>
     */
    private ResolvedTurn resolvePlayerLose(final TurnInput input) {
        if (RockPaperScissors.isNormalFamily(input.playerType())) {
            return resolveNormalLosesToDefense(input);
        }
        return resolveAttackLoses(input);
    }

    /**
     * 플레이어 공격 스킬이 상성 승리할 때 피해를 산출한다 (일반&gt;강, 강&gt;방어).
     *
     * <p>플레이어 100% 적중(멀티히트), 몬스터 피해 0.
     */
    private ResolvedTurn resolveAttackWins(final TurnInput input) {
        final List<HitResult> hits =
                multiHitDamage(
                        input.playerAttackPower(),
                        input.playerMultiplierPercent(),
                        input.monsterDefense(),
                        WIN_COEFFICIENT,
                        input.playerCritical(),
                        input.playerHitCount(),
                        input.playerDefensePierce());
        final int totalDamage = sumDamage(hits);
        final boolean anyCrit = anyCritical(hits);

        return new ResolvedTurn(totalDamage, 0, anyCrit, false, false, false, hits);
    }

    /**
     * 카운터 어택이 공격(일반 또는 강공격)을 상대로 성공할 때 피해를 산출한다.
     *
     * <p>플레이어 피격은 0(완전 회피/흘리기), 반격 피해는 플레이어 공격력 비례({@code playerAttackPower × counterPercent})로
     * 산출한다.
     */
    private ResolvedTurn resolveCounterAttackWins(final TurnInput input) {
        final int counterDamage =
                calculateCounterDamage(
                        input.playerAttackPower(),
                        input.playerCounterPercent(),
                        input.monsterDefense(),
                        input.playerCritical());
        final boolean playerCrit = rollCritical(input.playerCritical());
        final boolean monsterCrit = rollCritical(input.monsterCritical());

        return new ResolvedTurn(
                counterDamage, 0, playerCrit, monsterCrit, false, counterDamage > 0, List.of());
    }

    /**
     * 방어가 일반을 이길 때 피해를 산출한다 (방어자=플레이어, 공격자=몬스터).
     *
     * <p>몬스터 피해를 경감률로 줄이고, 플레이어는 반격 피해를 가한다. 반격·방어 경로는 단일 히트이며 playerHits는 비어 있다.
     */
    private ResolvedTurn resolveDefenseWinsNormal(final TurnInput input) {
        final int monsterBase =
                baseDamage(
                        input.monsterAttackPower(),
                        input.monsterMultiplierPercent(),
                        input.playerDefense());
        final double blockCoeff = 1.0 - input.playerBlockRatePercent() / 100.0;
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, blockCoeff, monsterCrit);

        final int counterDamage =
                calculateCounterDamage(
                        input.playerAttackPower(),
                        input.playerCounterPercent(),
                        input.monsterDefense(),
                        input.playerCritical());

        return new ResolvedTurn(
                counterDamage,
                monsterDmg,
                rollCritical(input.playerCritical()),
                monsterCrit,
                false,
                counterDamage > 0,
                List.of());
    }

    /**
     * 일반이 방어에 패배할 때 피해를 산출한다 (공격자=플레이어, 방어자=몬스터).
     *
     * <p>플레이어 피해를 몬스터 경감률로 줄인 뒤 멀티히트 적용, 몬스터는 반격 피해를 가한다.
     */
    private ResolvedTurn resolveNormalLosesToDefense(final TurnInput input) {
        final double blockCoeff = 1.0 - input.monsterBlockRatePercent() / 100.0;
        final List<HitResult> hits =
                multiHitDamage(
                        input.playerAttackPower(),
                        input.playerMultiplierPercent(),
                        input.monsterDefense(),
                        blockCoeff,
                        input.playerCritical(),
                        input.playerHitCount(),
                        input.playerDefensePierce());
        final int totalDamage = sumDamage(hits);
        final boolean anyCrit = anyCritical(hits);

        final int counterDamage =
                calculateCounterDamage(
                        input.monsterAttackPower(),
                        input.monsterCounterPercent(),
                        input.playerDefense(),
                        input.monsterCritical());

        return new ResolvedTurn(
                totalDamage,
                counterDamage,
                anyCrit,
                rollCritical(input.monsterCritical()),
                true,
                counterDamage > 0,
                hits);
    }

    /**
     * 플레이어 공격 스킬이 상성 패배할 때 피해를 산출한다 (강&lt;일반, 방어&lt;강).
     *
     * <p>플레이어 피해 0, 몬스터 100% 적중. playerHits는 비어 있다.
     */
    private ResolvedTurn resolveAttackLoses(final TurnInput input) {
        final int monsterBase =
                baseDamage(
                        input.monsterAttackPower(),
                        input.monsterMultiplierPercent(),
                        input.playerDefense());
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, WIN_COEFFICIENT, monsterCrit);

        return new ResolvedTurn(0, monsterDmg, false, monsterCrit, false, false, List.of());
    }

    /**
     * 동일 타입 무승부 시 피해를 산출한다.
     *
     * <p>방어 vs 방어는 교착(양쪽 0), 그 외 동일 타입은 양쪽 50% 피해.
     */
    private ResolvedTurn resolveDraw(final TurnInput input) {
        if (input.playerType() == SkillType.DEFENSE) {
            return new ResolvedTurn(0, 0, false, false, false, false, List.of());
        }
        return resolveDrawAttack(input);
    }

    /**
     * 공격 타입 동일(무승부) 시 양쪽 50% 피해를 산출한다.
     *
     * <p>플레이어는 멀티히트 적용, 몬스터는 단일 히트.
     */
    private ResolvedTurn resolveDrawAttack(final TurnInput input) {
        final List<HitResult> hits =
                multiHitDamage(
                        input.playerAttackPower(),
                        input.playerMultiplierPercent(),
                        input.monsterDefense(),
                        DRAW_COEFFICIENT,
                        input.playerCritical(),
                        input.playerHitCount(),
                        input.playerDefensePierce());
        final int playerDmg = sumDamage(hits);
        final boolean playerCrit = anyCritical(hits);

        final int monsterBase =
                baseDamage(
                        input.monsterAttackPower(),
                        input.monsterMultiplierPercent(),
                        input.playerDefense());
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, DRAW_COEFFICIENT, monsterCrit);

        return new ResolvedTurn(playerDmg, monsterDmg, playerCrit, monsterCrit, false, false, hits);
    }

    /**
     * 반격 피해를 산출한다.
     *
     * <p>반격 공식: {@code max(1, round(attackPower × counterPercent / 100 − targetDefense))}. 크리티컬과
     * 편차를 적용한다. 반격은 항상 단일 히트이며 counterPercent가 0 이하이면 0을 반환한다.
     */
    private int calculateCounterDamage(
            final int attackPower,
            final int counterPercent,
            final int targetDefense,
            final int critical) {
        if (counterPercent <= 0 || attackPower <= 0) {
            return 0;
        }
        final int counterBase =
                Math.max(1, Math.floorDiv(attackPower * counterPercent, 100) - targetDefense);
        final boolean counterCrit = rollCritical(critical);
        return finalDamage(counterBase, WIN_COEFFICIENT, counterCrit);
    }

    /**
     * 0.90 ~ 1.10 범위의 편차 값을 산출한다.
     *
     * <p>주입된 {@link Random}에서 0~200 범위의 정수를 뽑아 (900 + roll) / 1000.0으로 변환한다.
     */
    private double rollVariance() {
        final int roll = random.nextInt(VARIANCE_RANGE_MILLIS + 1);
        return (VARIANCE_BASE_MILLIS + roll) / MILLIS_DIVISOR;
    }

    /**
     * 히트 리스트의 피해 합계를 산출한다.
     *
     * @param hits 히트 결과 리스트
     * @return 총 피해량
     */
    private int sumDamage(final List<HitResult> hits) {
        int total = 0;
        for (final HitResult hit : hits) {
            total += hit.damage();
        }
        return total;
    }

    /**
     * 히트 리스트 중 하나라도 크리티컬이 있는지 확인한다.
     *
     * @param hits 히트 결과 리스트
     * @return 크리티컬 히트 존재 시 {@code true}
     */
    private boolean anyCritical(final List<HitResult> hits) {
        for (final HitResult hit : hits) {
            if (hit.critical()) {
                return true;
            }
        }
        return false;
    }
}
