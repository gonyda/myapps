package com.myapps.web.myrpg.domain.service;

import java.util.Random;

import com.myapps.web.myrpg.domain.model.AffinityResult;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;

/**
 * 전투 데미지 계산을 담당하는 순수 도메인 서비스.
 *
 * <p>감산형 데미지 공식, 가위바위보 상성계수, 크리티컬 판정, 편차를 조합하여
 * 9칸 매트릭스 기반의 양측 피해를 산출한다. 결정적 부분(감산·상성·경감·반격)은
 * 순수 함수이며, 비결정적 부분(크리티컬·편차)만 주입된 {@link Random}을 사용한다.
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
     * <p>공식: {@code max(1, floor(attackPower × skillMultiplierPercent / 100) − targetDefense)}.
     * 방어가 산출 피해를 초과해도 최소 1을 보장한다.
     *
     * @param attackPower            공격력
     * @param skillMultiplierPercent 스킬 배율(%)
     * @param targetDefense          대상 방어력
     * @return 기본피해 (최소 1)
     */
    public int baseDamage(final int attackPower, final int skillMultiplierPercent, final int targetDefense) {
        final int rawDamage = Math.floorDiv(attackPower * skillMultiplierPercent, 100) - targetDefense;
        return Math.max(1, rawDamage);
    }

    /**
     * 상성계수를 반환한다.
     *
     * <p>상성 결과와 방어 관련 플래그에 따라 계수를 결정한다:
     * <ul>
     *   <li>WIN: 1.0 (100% 적중)</li>
     *   <li>DRAW: 0.5 (50% 피해)</li>
     *   <li>LOSE + penetrated: 0.0 (강이 방어를 뚫을 때 방어자 반격 무효)</li>
     *   <li>LOSE + not penetrated: (1 − blockRatePercent / 100) — 방어 경감 후 잔여 피해</li>
     * </ul>
     *
     * @param result           상성 판정 결과
     * @param penetrated       관통 여부 (강 vs 방어에서 방어측이 관통당했는지)
     * @param blockRatePercent 방어 경감률(%) — LOSE이고 비관통일 때만 사용
     * @return 상성계수 (0.0 ~ 1.0)
     */
    public double affinityCoefficient(final AffinityResult result, final boolean penetrated,
                                      final int blockRatePercent) {
        return switch (result) {
            case WIN -> WIN_COEFFICIENT;
            case DRAW -> DRAW_COEFFICIENT;
            case LOSE -> penetrated ? PENETRATED_COEFFICIENT : 1.0 - blockRatePercent / 100.0;
        };
    }

    /**
     * 크리티컬 판정을 수행한다.
     *
     * <p>주입된 {@link Random}으로 0~999 범위의 값을 뽑아, {@code critical} 수치보다
     * 작으면 크리티컬로 판정한다. critical 단위는 0.1% (예: 100 = 10%).
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
     * <p>공식: {@code max(1, round(baseDamage × affinityCoefficient × (critical ? 1.5 : 1.0) × variance))}.
     * variance는 0.90 ~ 1.10 범위의 균등 분포이며 주입된 {@link Random}으로 산출한다.
     *
     * @param baseDamage           기본피해
     * @param affinityCoefficient  상성계수
     * @param critical             크리티컬 발동 여부
     * @return 최종 피해 (최소 1)
     */
    public int finalDamage(final int baseDamage, final double affinityCoefficient, final boolean critical) {
        final double critMultiplier = critical ? CRITICAL_MULTIPLIER : 1.0;
        final double variance = rollVariance();
        final double rawDamage = baseDamage * affinityCoefficient * critMultiplier * variance;
        return Math.max(1, (int) Math.round(rawDamage));
    }

    /**
     * 9칸 매트릭스에 따라 양측 피해를 산출한다.
     *
     * <p>플레이어와 몬스터의 스킬 타입 조합에 따라 상성을 판정하고,
     * 각 조합별 규칙(경감·반격·관통·교착)을 적용한 뒤 크리티컬과 편차를 반영한
     * 최종 피해를 반환한다. 선후공 결정은 포함하지 않는다.
     *
     * @param input 턴 해결에 필요한 양측 수치 입력
     * @return 양측 피해와 플래그를 담은 해결 결과
     */
    public ResolvedTurn resolve(final TurnInput input) {
        final AffinityResult playerAffinity = RockPaperScissors.judge(input.playerType(), input.monsterType());

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
     * <ul>
     *   <li>일반 &gt; 강: 플레이어 100%, 몬스터 0</li>
     *   <li>강 &gt; 방어: 플레이어 100% 관통, 몬스터 반격 무효(0)</li>
     *   <li>방어 &gt; 일반: 플레이어 반격, 몬스터 경감 후 피해</li>
     * </ul>
     */
    private ResolvedTurn resolvePlayerWin(final TurnInput input) {
        if (input.playerType() == SkillType.DEFENSE) {
            return resolveDefenseWinsNormal(input);
        }
        return resolveAttackWins(input);
    }

    /**
     * 플레이어 상성 패배 시 피해를 산출한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>일반 &lt; 방어: 플레이어 경감 후 피해, 몬스터 반격</li>
     *   <li>강 &lt; 일반: 플레이어 0, 몬스터 100%</li>
     *   <li>방어 &lt; 강: 플레이어 반격 무효(0), 몬스터 100% 관통</li>
     * </ul>
     */
    private ResolvedTurn resolvePlayerLose(final TurnInput input) {
        if (input.playerType() == SkillType.NORMAL) {
            return resolveNormalLosesToDefense(input);
        }
        return resolveAttackLoses(input);
    }

    /**
     * 플레이어 공격 스킬이 상성 승리할 때 피해를 산출한다 (일반&gt;강, 강&gt;방어).
     *
     * <p>플레이어 100% 적중, 몬스터 피해 0.
     */
    private ResolvedTurn resolveAttackWins(final TurnInput input) {
        final int playerBase = baseDamage(input.playerAttackPower(),
                input.playerMultiplierPercent(), input.monsterDefense());
        final boolean playerCrit = rollCritical(input.playerCritical());
        final int playerDmg = finalDamage(playerBase, WIN_COEFFICIENT, playerCrit);

        return new ResolvedTurn(playerDmg, 0, playerCrit, false, false, false);
    }

    /**
     * 방어가 일반을 이길 때 피해를 산출한다 (방어자=플레이어, 공격자=몬스터).
     *
     * <p>몬스터 피해를 경감률로 줄이고, 플레이어는 반격 피해를 가한다.
     */
    private ResolvedTurn resolveDefenseWinsNormal(final TurnInput input) {
        final int monsterBase = baseDamage(input.monsterAttackPower(),
                input.monsterMultiplierPercent(), input.playerDefense());
        final double blockCoeff = 1.0 - input.playerBlockRatePercent() / 100.0;
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, blockCoeff, monsterCrit);

        final int counterDamage = calculateCounterDamage(input.playerAttackPower(),
                input.playerCounterPercent(), input.monsterDefense(), input.playerCritical());

        return new ResolvedTurn(counterDamage, monsterDmg, rollCritical(input.playerCritical()), monsterCrit,
                false, true);
    }

    /**
     * 일반이 방어에 패배할 때 피해를 산출한다 (공격자=플레이어, 방어자=몬스터).
     *
     * <p>플레이어 피해를 몬스터 경감률로 줄이고, 몬스터는 반격 피해를 가한다.
     */
    private ResolvedTurn resolveNormalLosesToDefense(final TurnInput input) {
        final int playerBase = baseDamage(input.playerAttackPower(),
                input.playerMultiplierPercent(), input.monsterDefense());
        final double blockCoeff = 1.0 - input.monsterBlockRatePercent() / 100.0;
        final boolean playerCrit = rollCritical(input.playerCritical());
        final int playerDmg = finalDamage(playerBase, blockCoeff, playerCrit);

        final int counterDamage = calculateCounterDamage(input.monsterAttackPower(),
                input.monsterCounterPercent(), input.playerDefense(), input.monsterCritical());

        return new ResolvedTurn(playerDmg, counterDamage, playerCrit, rollCritical(input.monsterCritical()),
                true, true);
    }

    /**
     * 플레이어 공격 스킬이 상성 패배할 때 피해를 산출한다 (강&lt;일반, 방어&lt;강).
     *
     * <p>플레이어 피해 0, 몬스터 100% 적중.
     */
    private ResolvedTurn resolveAttackLoses(final TurnInput input) {
        final int monsterBase = baseDamage(input.monsterAttackPower(),
                input.monsterMultiplierPercent(), input.playerDefense());
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, WIN_COEFFICIENT, monsterCrit);

        return new ResolvedTurn(0, monsterDmg, false, monsterCrit, false, false);
    }

    /**
     * 동일 타입 무승부 시 피해를 산출한다.
     *
     * <p>방어 vs 방어는 교착(양쪽 0), 그 외 동일 타입은 양쪽 50% 피해.
     */
    private ResolvedTurn resolveDraw(final TurnInput input) {
        if (input.playerType() == SkillType.DEFENSE) {
            return new ResolvedTurn(0, 0, false, false, false, false);
        }
        return resolveDrawAttack(input);
    }

    /**
     * 공격 타입 동일(무승부) 시 양쪽 50% 피해를 산출한다.
     */
    private ResolvedTurn resolveDrawAttack(final TurnInput input) {
        final int playerBase = baseDamage(input.playerAttackPower(),
                input.playerMultiplierPercent(), input.monsterDefense());
        final boolean playerCrit = rollCritical(input.playerCritical());
        final int playerDmg = finalDamage(playerBase, DRAW_COEFFICIENT, playerCrit);

        final int monsterBase = baseDamage(input.monsterAttackPower(),
                input.monsterMultiplierPercent(), input.playerDefense());
        final boolean monsterCrit = rollCritical(input.monsterCritical());
        final int monsterDmg = finalDamage(monsterBase, DRAW_COEFFICIENT, monsterCrit);

        return new ResolvedTurn(playerDmg, monsterDmg, playerCrit, monsterCrit, false, false);
    }

    /**
     * 반격 피해를 산출한다.
     *
     * <p>반격 공식: {@code max(1, round(attackPower × counterPercent / 100 − targetDefense))}.
     * 크리티컬과 편차를 적용한다.
     */
    private int calculateCounterDamage(final int attackPower, final int counterPercent,
                                       final int targetDefense, final int critical) {
        final int counterBase = Math.max(1, Math.floorDiv(attackPower * counterPercent, 100) - targetDefense);
        final boolean counterCrit = rollCritical(critical);
        return finalDamage(counterBase, WIN_COEFFICIENT, counterCrit);
    }

    /**
     * 0.90 ~ 1.10 범위의 편차 값을 산출한다.
     *
     * <p>주입된 {@link Random}에서 0~200 범위의 정수를 뽑아
     * (900 + roll) / 1000.0으로 변환한다.
     */
    private double rollVariance() {
        final int roll = random.nextInt(VARIANCE_RANGE_MILLIS + 1);
        return (VARIANCE_BASE_MILLIS + roll) / MILLIS_DIVISOR;
    }
}
