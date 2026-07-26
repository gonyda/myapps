package com.myapps.web.myrpg.application.service;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.service.BattleService;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;

import jakarta.servlet.http.HttpSession;

/**
 * 전투 세션 오케스트레이션 서비스.
 *
 * <p>전투 진행 상태(HP/MP·몬스터·턴)를 HTTP 세션에 유지하며 DB에는 저장하지 않는다.
 * 선공 결정 → 행동 처리 → 전투 종료 판정 순서를 HP가 0이 될 때까지 반복한다.
 * 보스 몬스터도 일반 몬스터와 동일한 기본공격 규칙을 적용한다 (Req 10.7).
 */
@Service
public class BattleSessionService {

    private static final String SESSION_KEY = "BATTLE_SESSION";
    private static final double BASIC_ATTACK_MULTIPLIER = 1.0;

    private final BattleService battleService;
    private final MasterDataLoader masterDataLoader;

    /**
     * BattleSessionService를 생성한다.
     *
     * @param battleService    전투 도메인 규칙 서비스
     * @param masterDataLoader 마스터 데이터 로더
     */
    public BattleSessionService(final BattleService battleService,
                                final MasterDataLoader masterDataLoader) {
        this.battleService = battleService;
        this.masterDataLoader = masterDataLoader;
    }

    /**
     * 전투를 시작하고 세션에 전투 상태를 저장한다.
     *
     * <p>선후공 순서를 결정(Req 8.4)하고, 몬스터 선공이면 즉시 몬스터 턴을 처리한다.
     *
     * @param session      HTTP 세션
     * @param player       플레이어 엔티티
     * @param stats        플레이어 유효 스탯
     * @param monsterId    몬스터 템플릿 ID
     * @return 초기화된 전투 세션 상태
     */
    public BattleSession startBattle(final HttpSession session, final Player player,
                                     final EffectiveStats stats, final long monsterId) {
        final MonsterTemplate monster = masterDataLoader.findMonster(monsterId);
        final TurnOrder turnOrder = battleService.decideTurnOrder(stats.speed(), monster.speed());

        final long playerId = player.getId() != null ? player.getId() : 0L;
        final BattleSession battleSession = new BattleSession(
                playerId, monsterId, monster.name(),
                monster.hp(), monster.attack(), monster.defense(),
                monster.speed(), monster.damageType(), turnOrder,
                stats.attack(), stats.defense(), stats.speed(),
                stats.critical(), stats.maxHp(), stats.damageType(),
                player.getHp(), player.getMp());

        if (turnOrder == TurnOrder.MONSTER_FIRST) {
            executeMonsterAttack(battleSession);
        }

        session.setAttribute(SESSION_KEY, battleSession);
        return battleSession;
    }

    /**
     * 플레이어 기본 공격을 실행한다.
     *
     * <p>공격 후 몬스터가 생존하고 전투가 지속되면 몬스터 턴을 처리한다.
     *
     * @param session HTTP 세션
     * @return 공격 결과를 반영한 데미지 결과
     */
    public DamageResult playerAttack(final HttpSession session) {
        final BattleSession battleSession = getActiveBattleSession(session);

        final EffectiveStats attackerStats = buildPlayerStats(battleSession);
        final DamageResult result = battleService.computeDamage(
                battleSession.getPlayerAttack(), BASIC_ATTACK_MULTIPLIER,
                battleSession.getPlayerDamageType(), battleSession.getMonsterDefense(),
                attackerStats);

        applyDamageToMonster(battleSession, result.damage());
        advanceTurnAfterPlayerAction(battleSession);
        session.setAttribute(SESSION_KEY, battleSession);
        return result;
    }

    /**
     * 플레이어 스킬 공격을 실행한다.
     *
     * <p>MP 소비 후 스킬 배율로 데미지를 산출한다. 공격 후 몬스터가
     * 생존하고 전투가 지속되면 몬스터 턴을 처리한다.
     *
     * @param session HTTP 세션
     * @param skillId 사용할 스킬 템플릿 ID
     * @return 스킬 공격 결과를 반영한 데미지 결과
     * @throws InsufficientMpException MP 부족 시
     */
    public DamageResult playerSkillAttack(final HttpSession session, final long skillId) {
        final BattleSession battleSession = getActiveBattleSession(session);
        final SkillTemplate skill = masterDataLoader.findSkill(skillId);

        validateAndConsumeMpInSession(battleSession, skill.mpCost());

        final EffectiveStats attackerStats = buildPlayerStats(battleSession);
        final DamageResult result = battleService.computeDamage(
                battleSession.getPlayerAttack(), skill.damageMultiplier(),
                skill.damageType(), battleSession.getMonsterDefense(),
                attackerStats);

        applyDamageToMonster(battleSession, result.damage());
        advanceTurnAfterPlayerAction(battleSession);
        session.setAttribute(SESSION_KEY, battleSession);
        return result;
    }

    /**
     * 플레이어가 포션을 사용한다.
     *
     * <p>HP 또는 MP를 회복한 뒤, 전투가 지속되면 몬스터 턴을 처리한다.
     *
     * @param session      HTTP 세션
     * @param inventory    사용할 인벤토리 항목
     * @param effectAmount 포션 회복량
     * @param maxValue     최대 허용값 (maxHp 또는 maxMp)
     * @param isHp         HP 포션이면 true, MP 포션이면 false
     * @return 회복 후 새 값
     */
    public int playerUsePotion(final HttpSession session, final PlayerInventory inventory,
                               final int effectAmount, final int maxValue, final boolean isHp) {
        final BattleSession battleSession = getActiveBattleSession(session);

        battleService.consumeItem(inventory);
        final int newValue = applyPotionInSession(battleSession, effectAmount, maxValue, isHp);

        advanceTurnAfterPlayerAction(battleSession);
        session.setAttribute(SESSION_KEY, battleSession);
        return newValue;
    }

    /**
     * 플레이어가 도주를 시도한다.
     *
     * <p>도주 성공 시 전투 상태를 PLAYER_FLED로 변경한다.
     * 실패 시 몬스터 턴을 처리한다.
     *
     * @param session HTTP 세션
     * @return 도주 성공 여부
     */
    public boolean playerFlee(final HttpSession session) {
        final BattleSession battleSession = getActiveBattleSession(session);

        final boolean fled = battleService.attemptFlee();
        if (fled) {
            battleSession.changeStatus(BattleSession.BattleStatus.PLAYER_FLED);
        } else {
            advanceTurnAfterPlayerAction(battleSession);
        }

        session.setAttribute(SESSION_KEY, battleSession);
        return fled;
    }

    /**
     * 몬스터 턴을 수동으로 처리한다.
     *
     * <p>몬스터는 기본공격만 수행한다 (Req 10.1, 10.7).
     *
     * @param session HTTP 세션
     * @return 몬스터 공격 데미지 결과
     */
    public DamageResult processMonsterTurn(final HttpSession session) {
        final BattleSession battleSession = getActiveBattleSession(session);
        final DamageResult result = executeMonsterAttack(battleSession);
        session.setAttribute(SESSION_KEY, battleSession);
        return result;
    }

    /**
     * 현재 전투 세션 상태를 조회한다.
     *
     * @param session HTTP 세션
     * @return 전투 세션 (없으면 null)
     */
    public BattleSession getBattleSession(final HttpSession session) {
        return (BattleSession) session.getAttribute(SESSION_KEY);
    }

    /**
     * 전투 세션을 정리한다.
     *
     * @param session HTTP 세션
     */
    public void clearBattleSession(final HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    /**
     * 전투가 진행 중인지 확인한다.
     *
     * @param session HTTP 세션
     * @return 전투 진행 중이면 true
     */
    public boolean isBattleActive(final HttpSession session) {
        final BattleSession battleSession = getBattleSession(session);
        return battleSession != null && battleSession.isOngoing();
    }

    /**
     * 세션에서 활성 전투 상태를 꺼내며, 전투 중이 아니면 예외를 던진다.
     */
    private BattleSession getActiveBattleSession(final HttpSession session) {
        final BattleSession battleSession = getBattleSession(session);
        if (battleSession == null || !battleSession.isOngoing()) {
            throw new IllegalActionException("진행 중인 전투가 없습니다.");
        }
        return battleSession;
    }

    /**
     * 몬스터 기본공격을 실행한다 (Req 10.1, 10.7 — 스킬 없이 기본공격만).
     */
    private DamageResult executeMonsterAttack(final BattleSession battleSession) {
        final DamageResult result = battleService.monsterDamage(
                battleSession.getMonsterAttack(),
                battleSession.getMonsterDamageType(),
                battleSession.getPlayerDefense());

        applyDamageToPlayer(battleSession, result.damage());
        return result;
    }

    /**
     * 플레이어 행동 후 턴을 진행한다.
     *
     * <p>몬스터가 생존하고 전투가 지속되면 몬스터 공격을 수행하고 턴을 증가시킨다.
     */
    private void advanceTurnAfterPlayerAction(final BattleSession battleSession) {
        if (battleSession.isOngoing() && battleSession.getMonsterCurrentHp() > 0) {
            executeMonsterAttack(battleSession);
        }
        if (battleSession.isOngoing()) {
            battleSession.incrementTurn();
        }
    }

    /**
     * 몬스터에 데미지를 적용하고 종료 판정을 수행한다.
     */
    private void applyDamageToMonster(final BattleSession battleSession, final int damage) {
        final int newHp = battleSession.getMonsterCurrentHp() - damage;
        battleSession.changeMonsterCurrentHp(newHp);
        if (battleSession.getMonsterCurrentHp() <= 0) {
            battleSession.changeStatus(BattleSession.BattleStatus.PLAYER_WON);
        }
    }

    /**
     * 플레이어에 데미지를 적용하고 종료 판정을 수행한다.
     */
    private void applyDamageToPlayer(final BattleSession battleSession, final int damage) {
        final int newHp = battleSession.getPlayerCurrentHp() - damage;
        battleSession.changePlayerCurrentHp(newHp);
        if (battleSession.getPlayerCurrentHp() <= 0) {
            battleSession.changeStatus(BattleSession.BattleStatus.PLAYER_DEAD);
        }
    }

    /**
     * 세션 내 MP를 검증하고 차감한다.
     */
    private void validateAndConsumeMpInSession(final BattleSession battleSession, final int mpCost) {
        if (battleSession.getPlayerCurrentMp() < mpCost) {
            throw new InsufficientMpException(
                    "MP가 부족합니다. 현재: " + battleSession.getPlayerCurrentMp()
                            + ", 필요: " + mpCost);
        }
        battleSession.changePlayerCurrentMp(battleSession.getPlayerCurrentMp() - mpCost);
    }

    /**
     * 세션 내 포션을 적용한다.
     */
    private int applyPotionInSession(final BattleSession battleSession,
                                     final int effectAmount, final int maxValue,
                                     final boolean isHp) {
        if (isHp) {
            final int newHp = Math.min(battleSession.getPlayerCurrentHp() + effectAmount, maxValue);
            battleSession.changePlayerCurrentHp(newHp);
            return newHp;
        } else {
            final int newMp = Math.min(battleSession.getPlayerCurrentMp() + effectAmount, maxValue);
            battleSession.changePlayerCurrentMp(newMp);
            return newMp;
        }
    }

    /**
     * BattleSession으로부터 EffectiveStats를 구성한다.
     */
    private EffectiveStats buildPlayerStats(final BattleSession battleSession) {
        return new EffectiveStats(
                battleSession.getPlayerAttack(),
                battleSession.getPlayerDefense(),
                battleSession.getPlayerSpeed(),
                battleSession.getPlayerCritical(),
                battleSession.getPlayerMaxHp(),
                battleSession.getPlayerDamageType());
    }
}
