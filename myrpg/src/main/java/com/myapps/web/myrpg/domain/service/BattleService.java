package com.myapps.web.myrpg.domain.service;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.random.RandomSource;

/**
 * 전투 관련 순수 도메인 규칙 서비스.
 *
 * <p>데미지 산출, 치명타, 선후공 결정, 도주, MP 관리, 포션 사용 등
 * 전투 중 발생하는 모든 계산 로직을 캡슐화한다.
 * 리포지토리 의존 없이 {@link RandomSource}만 주입받는 순수 서비스이다.
 */
@Service
public class BattleService {

    private static final int MIN_CRITICAL_CHANCE = 5;
    private static final int MAX_CRITICAL_CHANCE = 100;
    private static final double SPEED_CRITICAL_COEFFICIENT = 0.2;

    private static final double PHYSICAL_DEFENSE_COEFFICIENT = 0.5;
    private static final double MAGICAL_DEFENSE_COEFFICIENT = 0.2;

    private static final double DEVIATION_MIN = 0.9;
    private static final double DEVIATION_MAX = 1.1;

    private static final double CRITICAL_MULTIPLIER = 1.5;
    private static final int MIN_DAMAGE = 1;

    private static final double FLEE_THRESHOLD = 0.5;
    private static final double TURN_TIE_THRESHOLD = 0.5;

    private static final double MONSTER_SKILL_MULTIPLIER = 1.0;

    private final RandomSource randomSource;

    /**
     * BattleService를 생성한다.
     *
     * @param randomSource 난수 생성 인터페이스
     */
    public BattleService(final RandomSource randomSource) {
        this.randomSource = randomSource;
    }

    /**
     * 유효 스탯 기반 치명타 확률을 산출한다.
     *
     * <p>공식: {@code 5 + (speed × 0.2) + critical}, [5, 100] 범위로 클램프.
     *
     * @param stats 공격자의 유효 스탯
     * @return 치명타 확률 (5~100)
     */
    public int criticalChance(final EffectiveStats stats) {
        final double raw = MIN_CRITICAL_CHANCE
                + (stats.speed() * SPEED_CRITICAL_COEFFICIENT)
                + stats.critical();
        return Math.max(MIN_CRITICAL_CHANCE, Math.min(MAX_CRITICAL_CHANCE, (int) raw));
    }

    /**
     * 플레이어 공격 데미지를 산출한다.
     *
     * <p>기본 데미지 → 랜덤 편차 → 치명타 판정 → 최소 1 보장 순서로 처리한다.
     *
     * @param attackPower    공격력 (스킬 base 또는 weapon attack)
     * @param skillMultiplier 스킬 배율
     * @param damageType     데미지 타입 (PHYSICAL / MAGICAL)
     * @param targetDefense  대상 방어력
     * @param attackerStats  공격자 유효 스탯
     * @return 최종 데미지와 치명타 여부
     */
    public DamageResult computeDamage(final int attackPower, final double skillMultiplier,
                                      final DamageType damageType, final int targetDefense,
                                      final EffectiveStats attackerStats) {
        final double defenseCoefficient = defenseCoefficient(damageType);
        double damage = attackPower * skillMultiplier - targetDefense * defenseCoefficient;

        damage *= randomSource.nextDoubleInRange(DEVIATION_MIN, DEVIATION_MAX);

        final boolean isCritical = randomSource.nextDouble() * MAX_CRITICAL_CHANCE
                < criticalChance(attackerStats);
        if (isCritical) {
            damage *= CRITICAL_MULTIPLIER;
        }

        if (damage < MIN_DAMAGE) {
            damage = MIN_DAMAGE;
        }

        return new DamageResult((int) damage, isCritical);
    }

    /**
     * 선후공 순서를 결정한다.
     *
     * <p>속도가 높은 쪽이 선공, 동일하면 50% 확률로 결정한다.
     *
     * @param playerSpeed  플레이어 속도
     * @param monsterSpeed 몬스터 속도
     * @return 선후공 순서
     */
    public TurnOrder decideTurnOrder(final int playerSpeed, final int monsterSpeed) {
        if (playerSpeed > monsterSpeed) {
            return TurnOrder.PLAYER_FIRST;
        }
        if (playerSpeed < monsterSpeed) {
            return TurnOrder.MONSTER_FIRST;
        }
        return randomSource.nextDouble() < TURN_TIE_THRESHOLD
                ? TurnOrder.PLAYER_FIRST
                : TurnOrder.MONSTER_FIRST;
    }

    /**
     * 몬스터 공격 데미지를 산출한다.
     *
     * <p>플레이어 공격과 동일한 파이프라인이나 스킬 배율 1.0, 치명타 없음으로 처리한다.
     *
     * @param monsterAttack 몬스터 공격력
     * @param monsterType   몬스터 데미지 타입
     * @param playerDefense 플레이어 방어력
     * @return 최종 데미지 (치명타는 항상 false)
     */
    public DamageResult monsterDamage(final int monsterAttack, final DamageType monsterType,
                                      final int playerDefense) {
        final double defenseCoefficient = defenseCoefficient(monsterType);
        double damage = monsterAttack * MONSTER_SKILL_MULTIPLIER
                - playerDefense * defenseCoefficient;

        damage *= randomSource.nextDoubleInRange(DEVIATION_MIN, DEVIATION_MAX);

        if (damage < MIN_DAMAGE) {
            damage = MIN_DAMAGE;
        }

        return new DamageResult((int) damage, false);
    }

    /**
     * 도주 시도 결과를 반환한다.
     *
     * <p>50% 확률로 성공한다.
     *
     * @return 도주 성공 여부
     */
    public boolean attemptFlee() {
        return randomSource.nextDouble() < FLEE_THRESHOLD;
    }

    /**
     * 스킬 사용 시 MP를 검증하고 차감한다.
     *
     * <p>현재 MP가 비용 미만이면 {@link InsufficientMpException}을 던진다.
     *
     * @param player 스킬을 사용하는 플레이어
     * @param mpCost MP 비용
     * @throws InsufficientMpException MP 부족 시
     */
    public void validateAndConsumeMp(final Player player, final int mpCost) {
        if (player.getMp() < mpCost) {
            throw new InsufficientMpException(
                    "MP가 부족합니다. 현재: " + player.getMp() + ", 필요: " + mpCost);
        }
        player.changeMp(player.getMp() - mpCost);
    }

    /**
     * 전투 종료 후 MP를 최대치로 회복한다.
     *
     * @param player 회복할 플레이어
     */
    public void restoreMpAfterBattle(final Player player) {
        player.changeMp(player.getMaxMp());
    }

    /**
     * 포션을 적용하여 HP 또는 MP를 회복한다.
     *
     * <p>회복량은 최대치를 초과하지 않는다.
     *
     * @param player       포션을 사용하는 플레이어
     * @param effectAmount 포션 회복량
     * @param maxValue     최대 허용값 (maxHp 또는 maxMp)
     * @param isHp         HP 포션이면 true, MP 포션이면 false
     * @return 회복 후 새 값
     */
    public int applyPotion(final Player player, final int effectAmount,
                           final int maxValue, final boolean isHp) {
        final int current = isHp ? player.getHp() : player.getMp();
        final int newValue = Math.min(current + effectAmount, maxValue);

        if (isHp) {
            player.changeHp(newValue);
        } else {
            player.changeMp(newValue);
        }

        return newValue;
    }

    /**
     * 소모품 수량을 1 감소시킨다.
     *
     * @param inventory 사용할 인벤토리 항목
     */
    public void consumeItem(final PlayerInventory inventory) {
        inventory.changeQuantity(inventory.getQuantity() - 1);
    }

    /**
     * 데미지 타입에 따른 방어 계수를 반환한다.
     *
     * @param damageType 데미지 타입
     * @return 방어 계수 (물리 0.5, 마법 0.2)
     */
    private double defenseCoefficient(final DamageType damageType) {
        return damageType == DamageType.PHYSICAL
                ? PHYSICAL_DEFENSE_COEFFICIENT
                : MAGICAL_DEFENSE_COEFFICIENT;
    }
}
