package com.myapps.web.myrpg.domain.service;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;

/**
 * 캐릭터 성장·경험치·페널티를 담당하는 순수 도메인 서비스.
 *
 * <p>레벨업 공식, 경험치 페널티, 마을 복귀 회복, 초기 캐릭터 생성 등
 * 캐릭터 상태 변경 규칙을 캡슐화한다.
 */
@Service
public class CharacterService {

    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_HP = 100;
    private static final int INITIAL_MP = 50;
    private static final int INITIAL_ATTACK = 10;
    private static final int INITIAL_DEFENSE = 5;
    private static final int INITIAL_SPEED = 5;
    private static final int INITIAL_CRITICAL = 0;
    private static final int INITIAL_EXP = 0;
    private static final int INITIAL_GOLD = 0;

    private static final double EXP_BASE = 100.0;
    private static final double EXP_EXPONENT = 1.5;

    private static final int HP_PER_LEVEL = 20;
    private static final int MP_PER_LEVEL = 10;
    private static final int ATTACK_PER_LEVEL = 3;
    private static final int DEFENSE_PER_LEVEL = 2;
    private static final int SPEED_PER_LEVEL = 1;
    private static final int CRITICAL_PER_LEVEL = 1;

    /**
     * 레벨 N에서 N+1로 올리기 위한 필요 경험치를 산출한다.
     *
     * <p>공식: {@code round(100 × level^1.5)} (HALF_UP 반올림)
     *
     * @param level 현재 레벨 (1 이상)
     * @return 필요 경험치
     */
    public int requiredExp(final int level) {
        return (int) Math.round(EXP_BASE * Math.pow(level, EXP_EXPONENT));
    }

    /**
     * 플레이어에게 경험치를 지급하고 레벨업을 처리한다.
     *
     * <p>음수 경험치는 거부하여 상태를 변경하지 않는다.
     * 양수 경험치를 누적한 뒤, 현재 경험치가 필요 경험치 이상인 동안
     * 반복적으로 레벨업을 수행한다.
     *
     * @param player 경험치를 받을 플레이어
     * @param amount 지급할 경험치 (음수 시 무시)
     * @return 레벨업 결과(새 레벨, 상승 횟수, 남은 경험치)
     */
    public LevelUpResult gainExp(final Player player, final int amount) {
        final int originalLevel = player.getLevel();

        if (amount < 0) {
            return new LevelUpResult(originalLevel, 0, player.getExp());
        }

        player.changeExp(player.getExp() + amount);
        processLevelUps(player);

        final int levelsGained = player.getLevel() - originalLevel;
        return new LevelUpResult(player.getLevel(), levelsGained, player.getExp());
    }

    /**
     * 경험치 페널티를 적용한다.
     *
     * <p>현재 레벨 진행분(현재 경험치)에만 한정되며,
     * {@code floor(currentExp × ratio)}만큼 감소시킨다.
     * 결과가 0 미만이 되지 않으며 레벨은 불변이다.
     *
     * @param player 페널티를 받을 플레이어
     * @param ratio  감소 비율 (사망 0.10, 도망 0.05)
     * @return 실제 감소한 경험치량
     */
    public int applyExpPenalty(final Player player, final double ratio) {
        final int currentExp = player.getExp();

        if (currentExp <= 0) {
            return 0;
        }

        final int deduction = (int) Math.floor(currentExp * ratio);
        final int newExp = Math.max(0, currentExp - deduction);
        player.changeExp(newExp);

        return currentExp - newExp;
    }

    /**
     * 마을로 복귀 시 HP와 MP를 최대치로 회복한다.
     *
     * @param player 회복할 플레이어
     */
    public void restoreToTown(final Player player) {
        player.changeHp(player.getMaxHp());
        player.changeMp(player.getMaxMp());
    }

    /**
     * 초기 캐릭터를 생성한다.
     *
     * <p>Lv1, HP 100, MP 50, 공격력 10, 방어력 5, 속도 5,
     * 치명타 0, 경험치 0, 골드 0으로 설정된 새 플레이어를 반환한다.
     *
     * @param name 캐릭터명
     * @return 초기 상태의 플레이어 엔티티
     */
    public Player createInitialCharacter(final String name) {
        return new Player(
                name,
                INITIAL_LEVEL,
                INITIAL_EXP,
                INITIAL_HP,
                INITIAL_HP,
                INITIAL_MP,
                INITIAL_MP,
                INITIAL_ATTACK,
                INITIAL_DEFENSE,
                INITIAL_SPEED,
                INITIAL_CRITICAL,
                INITIAL_GOLD
        );
    }

    /**
     * 현재 경험치가 필요 경험치 이상인 동안 반복적으로 레벨업을 수행한다.
     *
     * <p>각 레벨업마다 필요 경험치를 차감하고, 레벨을 1 올리며,
     * 스탯을 증가시킨 뒤 HP/MP를 최대치로 회복한다.
     *
     * @param player 레벨업 대상 플레이어
     */
    private void processLevelUps(final Player player) {
        int required = requiredExp(player.getLevel());

        while (player.getExp() >= required) {
            player.changeExp(player.getExp() - required);
            player.changeLevel(player.getLevel() + 1);
            applyStatIncrease(player);
            restoreAfterLevelUp(player);
            required = requiredExp(player.getLevel());
        }
    }

    /**
     * 레벨업 시 기본 스탯을 증가시킨다.
     *
     * <p>HP +20, MP +10, 공격력 +3, 방어력 +2, 속도 +1, 치명타 +1
     *
     * @param player 스탯을 증가시킬 플레이어
     */
    private void applyStatIncrease(final Player player) {
        player.changeMaxHp(player.getMaxHp() + HP_PER_LEVEL);
        player.changeMaxMp(player.getMaxMp() + MP_PER_LEVEL);
        player.changeAttack(player.getAttack() + ATTACK_PER_LEVEL);
        player.changeDefense(player.getDefense() + DEFENSE_PER_LEVEL);
        player.changeSpeed(player.getSpeed() + SPEED_PER_LEVEL);
        player.changeCritical(player.getCritical() + CRITICAL_PER_LEVEL);
    }

    /**
     * 레벨업 후 현재 HP와 MP를 최대치로 회복한다.
     *
     * @param player 회복할 플레이어
     */
    private void restoreAfterLevelUp(final Player player) {
        player.changeHp(player.getMaxHp());
        player.changeMp(player.getMaxMp());
    }
}
