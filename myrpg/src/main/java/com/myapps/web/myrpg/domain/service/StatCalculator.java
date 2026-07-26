package com.myapps.web.myrpg.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerArmorStat;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;
import com.myapps.web.myrpg.domain.model.StatType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;

/**
 * 캐릭터의 유효 스탯을 계산하는 순수 도메인 서비스.
 *
 * <p>기본 스탯 + 무기 base값 + 장비 랜덤 스탯을 합산하여 전투에서 사용할
 * 최종 유효 스탯({@link EffectiveStats})을 산출한다.
 */
@Service
public class StatCalculator {

    /**
     * 플레이어의 유효 전투 스탯을 계산한다.
     *
     * <p>계산 공식:
     * <ul>
     *   <li>유효 공격력 = 캐릭터 공격력 + 무기 base_attack + (무기 랜덤 ATTACK 합 + 방어구 랜덤 ATTACK 합)</li>
     *   <li>유효 방어력 = 캐릭터 방어력 + 착용 방어구 baseDefense 합 + (무기 랜덤 DEFENSE 합 + 방어구 랜덤 DEFENSE 합)</li>
     *   <li>유효 속도 = 캐릭터 속도 + 무기 base_speed + (무기 랜덤 SPEED 합 + 방어구 랜덤 SPEED 합)</li>
     *   <li>유효 치명타 = 캐릭터 치명타 + 무기 base_critical + (무기 랜덤 CRITICAL 합 + 방어구 랜덤 CRITICAL 합)</li>
     *   <li>유효 최대 HP = 캐릭터 최대 HP + (무기 랜덤 HP 합 + 방어구 랜덤 HP 합)</li>
     *   <li>데미지 타입 = 무기 타입이 STAFF → MAGICAL, 그 외 → PHYSICAL, 무기 없으면 PHYSICAL</li>
     * </ul>
     *
     * @param player        플레이어 캐릭터
     * @param equippedWeapon 착용 중인 무기 (없으면 null)
     * @param weaponStats   착용 무기의 랜덤 능력치 목록
     * @param equippedArmors 착용 중인 방어구 목록
     * @param armorStats    착용 방어구들의 랜덤 능력치 목록
     * @return 합산된 유효 전투 스탯
     */
    public EffectiveStats compute(final Player player,
                                  final PlayerWeapon equippedWeapon,
                                  final List<PlayerWeaponStat> weaponStats,
                                  final List<PlayerArmor> equippedArmors,
                                  final List<PlayerArmorStat> armorStats) {

        final int weaponBaseAttack = equippedWeapon != null ? equippedWeapon.getBaseAttack() : 0;
        final int weaponBaseSpeed = equippedWeapon != null ? equippedWeapon.getBaseSpeed() : 0;
        final int weaponBaseCritical = equippedWeapon != null ? equippedWeapon.getBaseCritical() : 0;

        final int randomAttack = sumWeaponStat(weaponStats, StatType.ATTACK) + sumArmorStat(armorStats, StatType.ATTACK);
        final int randomDefense = sumWeaponStat(weaponStats, StatType.DEFENSE) + sumArmorStat(armorStats, StatType.DEFENSE);
        final int randomSpeed = sumWeaponStat(weaponStats, StatType.SPEED) + sumArmorStat(armorStats, StatType.SPEED);
        final int randomCritical = sumWeaponStat(weaponStats, StatType.CRITICAL) + sumArmorStat(armorStats, StatType.CRITICAL);
        final int randomHp = sumWeaponStat(weaponStats, StatType.HP) + sumArmorStat(armorStats, StatType.HP);

        final int armorBaseDefense = equippedArmors.stream()
                .mapToInt(PlayerArmor::getBaseDefense)
                .sum();

        final int effectiveAttack = player.getAttack() + weaponBaseAttack + randomAttack;
        final int effectiveDefense = player.getDefense() + armorBaseDefense + randomDefense;
        final int effectiveSpeed = player.getSpeed() + weaponBaseSpeed + randomSpeed;
        final int effectiveCritical = player.getCritical() + weaponBaseCritical + randomCritical;
        final int effectiveMaxHp = player.getMaxHp() + randomHp;

        final DamageType damageType = determineDamageType(equippedWeapon);

        return new EffectiveStats(effectiveAttack, effectiveDefense, effectiveSpeed,
                effectiveCritical, effectiveMaxHp, damageType);
    }

    /**
     * 무기 랜덤 스탯 목록에서 지정 타입의 수치 합계를 구한다.
     *
     * @param stats    무기 랜덤 스탯 목록
     * @param statType 합산할 능력치 종류
     * @return 해당 종류의 수치 합계
     */
    private int sumWeaponStat(final List<PlayerWeaponStat> stats, final StatType statType) {
        return stats.stream()
                .filter(s -> s.getStatType() == statType)
                .mapToInt(PlayerWeaponStat::getStatValue)
                .sum();
    }

    /**
     * 방어구 랜덤 스탯 목록에서 지정 타입의 수치 합계를 구한다.
     *
     * @param stats    방어구 랜덤 스탯 목록
     * @param statType 합산할 능력치 종류
     * @return 해당 종류의 수치 합계
     */
    private int sumArmorStat(final List<PlayerArmorStat> stats, final StatType statType) {
        return stats.stream()
                .filter(s -> s.getStatType() == statType)
                .mapToInt(PlayerArmorStat::getStatValue)
                .sum();
    }

    /**
     * 착용 무기의 타입에 따라 데미지 타입을 결정한다.
     *
     * <p>STAFF 타입이면 MAGICAL, 그 외(또는 무기 미착용)이면 PHYSICAL을 반환한다.
     *
     * @param weapon 착용 중인 무기 (null 가능)
     * @return 결정된 데미지 타입
     */
    private DamageType determineDamageType(final PlayerWeapon weapon) {
        if (weapon == null) {
            return DamageType.PHYSICAL;
        }
        return weapon.getWeaponType() == WeaponType.STAFF ? DamageType.MAGICAL : DamageType.PHYSICAL;
    }
}
